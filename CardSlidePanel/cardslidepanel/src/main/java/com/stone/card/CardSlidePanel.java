package com.stone.card;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Point;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Adapter;

import java.util.ArrayList;
import java.util.List;

/**
 * 卡片滑动面板，主要逻辑实现类
 *
 * @author xmuSistone
 */
@SuppressLint({"HandlerLeak", "NewApi", "ClickableViewAccessibility"})
public class CardSlidePanel extends ViewGroup {
    private static final String TAG = CardSlidePanel.class.getSimpleName();
//    private List<CardItemView> viewList = new ArrayList<CardItemView>(); // 存放的是每一层的view，从顶到底
    private List<View> viewList = new ArrayList<>(); // 存放的是每一层的view，从顶到底
    private List<View> releasedViewList = new ArrayList<>(); // 手指松开后存放的view列表
    private List<CardItemHelper> helperList = new ArrayList<>(); // 帮助类的列表
    private List<CardItemHelper> releasedHelperList = new ArrayList<>(); // 手指松开后存放的帮助类的列表

    /* 拖拽工具类 */
    private ViewDragHelper mDragHelper; // 这个跟原生的ViewDragHelper差不多，我仅仅只是修改了Interpolator
    private int initCenterViewX = 0, initCenterViewY = 0; // 最初时，中间View的x位置,y位置
    private int allWidth = 0; // 面板的宽度
    private int allHeight = 0; // 面板的高度
    private int childWith = 0; // 每一个子View对应的宽度

    private static final float SCALE_STEP = 0.08f; // view叠加缩放的步长
    private static final int MAX_SLIDE_DISTANCE_LINKAGE = 500; // 水平距离+垂直距离

    // 超过这个值
    // 则下一层view完成向上一层view的过渡
//    private View bottomLayout; // 卡片下边的三个按钮布局
//    private View leftBtn, rightBtn;

    private View headerLayout; // header 布局
    private View footerLayout; // footer 布局


    private int itemMarginTop = 10; // 卡片距离顶部的偏移量
    private int bottomMarginTop = 40; // 底部按钮与卡片的margin值
    private int yOffsetStep = 40; // view叠加垂直偏移量的步长
    private int mTouchSlop = 5; // 判定为滑动的阈值，单位是像素
    private int mMaxVisibleItemSize = 3; //最大可见item数

    private static final int X_VEL_THRESHOLD = 800;
    private static final int X_DISTANCE_THRESHOLD = 300;

    public static final int VANISH_TYPE_LEFT = 0;
    public static final int VANISH_TYPE_RIGHT = 1;

    private Object obj1 = new Object();
    private boolean isInOrder = false; //是否处于排序中

    private Adapter mAdapter;
    private AdapterDataSetObserver mDataSetObserver;
    private CardSwitchListener cardSwitchListener; // 回调接口
//    private List<CardDataItem> dataList; // 存储的数据链表
    private int isShowing = 0; // 当前正在显示的小项
    private boolean btnLock = false;
    private GestureDetectorCompat moveDetector;
    private OnClickListener btnListener;
    private Point downPoint = new Point();

    public CardSlidePanel(Context context) {
        this(context, null);
    }

    public CardSlidePanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CardSlidePanel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.card);

        itemMarginTop = (int) a.getDimension(R.styleable.card_itemMarginTop, itemMarginTop);
        bottomMarginTop = (int) a.getDimension(R.styleable.card_bottomMarginTop, bottomMarginTop);
        yOffsetStep = (int) a.getDimension(R.styleable.card_yOffsetStep, yOffsetStep);

        // 滑动相关类
        mDragHelper = ViewDragHelper
                .create(this, 10f, new DragHelperCallback());
        mDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_BOTTOM);
        a.recycle();

        btnListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (null != cardSwitchListener && view.getScaleX() > 1 - SCALE_STEP) {
                    cardSwitchListener.onItemClick(view, isShowing);
                }
            }
        };

        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        moveDetector = new GestureDetectorCompat(context,
                new MoveDetector());
        moveDetector.setIsLongpressEnabled(false);
    }

    private class AdapterDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            updateData();
        }

        @Override
        public void onInvalidated() {
            updateData();
        }
    }

    private void updateData() {
        if(mAdapter != null) {
            initByAdapter();
        }
    }

    public Adapter getAdapter() {
        return mAdapter;
    }

    public void setAdapter(Adapter adapter) {
        if(mAdapter != null && mDataSetObserver != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
            mDataSetObserver = null;
        }

        mAdapter = adapter;
        isShowing = 0;

        if(mAdapter != null && mDataSetObserver == null) {
            mDataSetObserver = new AdapterDataSetObserver();
            mAdapter.registerDataSetObserver(mDataSetObserver);
        }

        if(viewList.size() == 0) {
            initByAdapter();
            requestLayout();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        headerLayout = findViewById(R.id.cardheader);
        footerLayout = findViewById(R.id.cardfooter);

    }

    private void clearView(List<View> views) {
        if(views != null && views.size() > 0) {
            for(View view : views) {
                removeView(view);
            }
        }
    }

    private void initByAdapter() {
        clearView(viewList);
        clearView(releasedViewList);
        viewList.clear();
        helperList.clear();
        releasedViewList.clear();
        releasedHelperList.clear();
        isShowing = 0;

        int size = mAdapter.getCount();
        if(size > 0) {
            //根据数据大小，初始化card view的个数，最大为mMaxVisibleItemSize + 1
            int maxSize = mMaxVisibleItemSize + 1;
            size = size > maxSize ? maxSize : size;
            for (int i = size - 1; i >= 0; i--) {
                View viewItem = mAdapter.getView(i, null, this);
                CardItemHelper helper = new CardItemHelper(viewItem);
                helper.setParentView(this);
                viewList.add(0, viewItem);
                helperList.add(0, helper);
                addView(viewItem);
                viewItem.setVisibility(View.VISIBLE);
            }

            if(viewList.size() == maxSize) {
                View bottomCardView = viewList.get(viewList.size() - 1);
                bottomCardView.setAlpha(0);
            }

            if (null != cardSwitchListener) {
                cardSwitchListener.onShow(0);
            }

            requestLayout();
            orderViewStack();
        }
    }

    class MoveDetector extends SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx,
                                float dy) {
            // 拖动了，touch不往下传递
            return Math.abs(dy) + Math.abs(dx) > mTouchSlop;
        }
    }


    /**
     * 这是viewdraghelper拖拽效果的主要逻辑
     */
    private class DragHelperCallback extends ViewDragHelper.Callback {

        @Override
        public void onViewPositionChanged(View changedView, int left, int top,
                                          int dx, int dy) {
            onViewPosChanged(changedView);
        }

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            // 如果数据List为空，或者子View不可见，则不予处理

            if (child.getId() == R.id.cardheader
                    || child.getId() == R.id.cardfooter
                    || (mAdapter != null && mAdapter.getCount() == 0)
                    || child.getVisibility() != View.VISIBLE
                    || child.getScaleX() <= 1.0f - SCALE_STEP) {
                // 一般来讲，如果拖动的是第三层、或者第四层的View，则直接禁止
                // 此处用getScale的用法来巧妙回避
                return false;
            }

            if (btnLock) {
                return false;
            }

            // 只捕获顶部view(rotation=0)
            int childIndex = viewList.indexOf(child);
            if (childIndex > 0) {
                return false;
            }

            CardItemHelper helper = helperList.get(childIndex);
            helper.onStartDragging();
            return helper.shouldCapture(downPoint.x, downPoint.y);
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            // 这个用来控制拖拽过程中松手后，自动滑行的速度
            return 256;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            animToSide(releasedChild, (int) xvel, (int) yvel);
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            return left;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return top;
        }
    }


    public void onViewPosChanged(View changedView) {
        // 调用offsetLeftAndRight导致viewPosition改变，会调到此处，所以此处对index做保护处理
        int index = viewList.indexOf(changedView);
        Log.e(TAG, "new onViewPosChanged, index=" + index  + " viewListSize=" + viewList.size());
        if (index + 2 > viewList.size()) {
            return;
        }

        processLinkageView(changedView);
    }

    /**
     * 对View重新排序
     */
    private void orderViewStack() {
        isInOrder = true;
        synchronized (obj1) {
            if (releasedViewList.size() == 0 || mAdapter == null) {
                isInOrder = false;
                return;
            }

            CardItemHelper helper = releasedHelperList.get(0);
            View changedView = releasedViewList.get(0);
            if (changedView.getLeft() == initCenterViewX) {
                releasedViewList.remove(0);
                releasedHelperList.remove(0);
                isInOrder = false;
                return;
            }

            // 1. 消失的卡片View位置重置，由于大多手机会重新调用onLayout函数，所以此处大可以不做处理，不信你注释掉看看
            changedView.offsetLeftAndRight(initCenterViewX
                    - changedView.getLeft());
            changedView.offsetTopAndBottom(initCenterViewY
                    - changedView.getTop() + yOffsetStep * 2);
            float scale = 1.0f - SCALE_STEP * 2;
            changedView.setScaleX(scale);
            changedView.setScaleY(scale);
            changedView.setAlpha(0);

            // 2. 卡片View在ViewGroup中的顺次调整
            int num = viewList.size();
            for (int i = num - 1; i > 0; i--) {
                View tempView = viewList.get(i);
                tempView.setAlpha(1);
                tempView.bringToFront();
            }

            // 3. changedView填充新数据
            int newIndex = isShowing + 4;
            if (newIndex < mAdapter.getCount()) {
                mAdapter.getView(newIndex, changedView, this);
            } else {
                changedView.setVisibility(View.INVISIBLE);
            }

            // 4. viewList中的卡片view的位次调整
            viewList.remove(changedView);
            viewList.add(changedView);
            helperList.remove(helper);
            helperList.add(helper);
            releasedViewList.remove(0);
            releasedHelperList.remove(0);

            // 5. 更新showIndex、接口回调
            if (isShowing + 1 < mAdapter.getCount()) {
                isShowing++;
            }
            if (null != cardSwitchListener) {
                cardSwitchListener.onShow(isShowing);
            }

            isInOrder = false;
        }
    }

    /**
     * 顶层卡片View位置改变，底层的位置需要调整
     *
     * @param changedView 顶层的卡片view
     */
    private void processLinkageView(View changedView) {
        int changeViewLeft = changedView.getLeft();
        int changeViewTop = changedView.getTop();
        int distance = Math.abs(changeViewTop - initCenterViewY)
                + Math.abs(changeViewLeft - initCenterViewX);
        float rate = distance / (float) MAX_SLIDE_DISTANCE_LINKAGE;

        float rate1 = rate;
        float rate2 = rate - 0.2f;

        if (rate > 1) {
            rate1 = 1;
        }

        if (rate2 < 0) {
            rate2 = 0;
        } else if (rate2 > 1) {
            rate2 = 1;
        }

        ajustLinkageViewItem(changedView, rate1, 1);
        int size = viewList.size();
        if(size > 2) {
            ajustLinkageViewItem(changedView, rate2, 2);
            if(size > 3) {
                View bottomCardView = viewList.get(viewList.size() - 1);
                bottomCardView.setAlpha(rate2);
            }
        }
    }

    // 由index对应view变成index-1对应的view
    private void ajustLinkageViewItem(View changedView, float rate, int index) {
        int changeIndex = viewList.indexOf(changedView);
        int initPosY = yOffsetStep * index;
        float initScale = 1 - SCALE_STEP * index;

        int nextPosY = yOffsetStep * (index - 1);
        float nextScale = 1 - SCALE_STEP * (index - 1);

        int offset = (int) (initPosY + (nextPosY - initPosY) * rate);
        float scale = initScale + (nextScale - initScale) * rate;

        View ajustView = viewList.get(changeIndex + index);
        ajustView.offsetTopAndBottom(offset - ajustView.getTop()
                + initCenterViewY);
        ajustView.setScaleX(scale);
        ajustView.setScaleY(scale);
    }

    /**
     * 松手时处理滑动到边缘的动画
     */
    private void animToSide(View changedView, int xvel, int yvel) {
        int finalX = initCenterViewX;
        int finalY = initCenterViewY;
        int flyType = -1;

        // 1. 下面这一坨计算finalX和finalY，要读懂代码需要建立一个比较清晰的数学模型才能理解，不信拉倒
        int dx = changedView.getLeft() - initCenterViewX;
        int dy = changedView.getTop() - initCenterViewY;

        // yvel < xvel * xyRate则允许以速度计算偏移
        final float xyRate = 3f;
        if (xvel > X_VEL_THRESHOLD && Math.abs(yvel) < xvel * xyRate) {
            // x正方向的速度足够大，向右滑动消失
            finalX = allWidth;
            finalY = yvel * (childWith + changedView.getLeft()) / xvel + changedView.getTop();
            flyType = VANISH_TYPE_RIGHT;
        } else if (xvel < -X_VEL_THRESHOLD && Math.abs(yvel) < -xvel * xyRate) {
            // x负方向的速度足够大，向左滑动消失
            finalX = -childWith;
            finalY = yvel * (childWith + changedView.getLeft()) / (-xvel) + changedView.getTop();
            flyType = VANISH_TYPE_LEFT;
        } else if (dx > X_DISTANCE_THRESHOLD && Math.abs(dy) < dx * xyRate) {
            // x正方向的位移足够大，向右滑动消失
            finalX = allWidth;
            finalY = dy * (childWith + initCenterViewX) / dx + initCenterViewY;
            flyType = VANISH_TYPE_RIGHT;
        } else if (dx < -X_DISTANCE_THRESHOLD && Math.abs(dy) < -dx * xyRate) {
            // x负方向的位移足够大，向左滑动消失
            finalX = -childWith;
            finalY = dy * (childWith + initCenterViewX) / (-dx) + initCenterViewY;
            flyType = VANISH_TYPE_LEFT;
        }

        // 如果斜率太高，就折中处理
        if (finalY > allHeight) {
            finalY = allHeight;
        } else if (finalY < -allHeight / 2) {
            finalY = -allHeight / 2;
        }

        // 如果没有飞向两侧，而是回到了中间，需要谨慎处理
        CardItemHelper helper = helperList.get(viewList.indexOf(changedView));
        if (finalX == initCenterViewX) {
            helper.animTo(initCenterViewX, initCenterViewY);
        } else {
            // 2. 向两边消失的动画
            releasedViewList.add(changedView);
            releasedHelperList.add(helper);
            if (mDragHelper.smoothSlideViewTo(changedView, finalX, finalY)) {
                ViewCompat.postInvalidateOnAnimation(this);
            }

            // 3. 消失动画即将进行，listener回调
            if (flyType >= 0 && cardSwitchListener != null) {
                cardSwitchListener.onCardVanish(isShowing, flyType);
            }
        }
    }

    /**
     * 点击按钮消失动画
     */
    public void vanishOnBtnClick(int type) {
        btnLock = true;

        synchronized (obj1) {
            if(helperList.size() == 0) {
                return ;
            }

            CardItemHelper helper = helperList.get(0);
            View animateView = helper.getView();
            if (animateView.getVisibility() != View.VISIBLE || releasedViewList.contains(animateView)) {
                return;
            }

            int finalX = 0;
            int extraVanishDistance = 100; // 为加快vanish的速度，额外添加消失的距离
            if (type == VANISH_TYPE_LEFT) {
                finalX = -childWith - extraVanishDistance;
            } else if (type == VANISH_TYPE_RIGHT) {
                finalX = allWidth + extraVanishDistance;
            }

            if (finalX != 0) {
                releasedViewList.add(animateView);
                releasedHelperList.add(helper);
                if (mDragHelper.smoothSlideViewTo(animateView, finalX, initCenterViewY + allHeight / 2)) {
                    ViewCompat.postInvalidateOnAnimation(this);
                }
            }

            if (type >= 0 && cardSwitchListener != null) {
                cardSwitchListener.onCardVanish(isShowing, type);
            }
        }
    }

    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        } else {
            // 动画结束
            synchronized (this) {
                if (mDragHelper.getViewDragState() == ViewDragHelper.STATE_IDLE) {
                    orderViewStack();
                    btnLock = false;
                }
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        // 按下时保存坐标信息
        if (action == MotionEvent.ACTION_DOWN) {
            this.downPoint.x = (int) ev.getX();
            this.downPoint.y = (int) ev.getY();
        }
        return super.dispatchTouchEvent(ev);
    }

    /* touch事件的拦截与处理都交给mDraghelper来处理 */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean shouldIntercept = mDragHelper.shouldInterceptTouchEvent(ev);
        boolean moveFlag = moveDetector.onTouchEvent(ev);
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN && !isInOrder) {
            // ACTION_DOWN的时候就对view重新排序
            if (mDragHelper.getViewDragState() == ViewDragHelper.STATE_SETTLING) {
                mDragHelper.abort();
            }
            orderViewStack();

            // 保存初次按下时arrowFlagView的Y坐标
            // action_down时就让mDragHelper开始工作，否则有时候导致异常
            mDragHelper.processTouchEvent(ev);
        }

        return shouldIntercept && moveFlag;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        try {
            // 统一交给mDragHelper处理，由DragHelperCallback实现拖动效果
            // 该行代码可能会抛异常，正式发布时请将这行代码加上try catch
            mDragHelper.processTouchEvent(e);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
        int maxHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(
                resolveSizeAndState(maxWidth, widthMeasureSpec, 0),
                resolveSizeAndState(maxHeight, heightMeasureSpec, 0));

        allWidth = getMeasuredWidth();
        allHeight = getMeasuredHeight();

        Log.e(TAG, "allWidth=" + allWidth + " allHeight=" + allHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
                            int bottom) {
//        Log.d(TAG, "layout: left=" + left + " top=" + top + " right=" + right + " bottom=" + bottom);
        // 布局header view
        int headerTotalHeight = 0;
        if(null != headerLayout) {
            int headerHeight = headerLayout.getMeasuredHeight();
            ViewGroup.LayoutParams lp = headerLayout.getLayoutParams();

            int headerMarginTop = 0;//lp.topMargin;
            int headerMarginBottom = 0;//lp.bottomMargin;
            headerLayout.layout(left, headerMarginTop, right, headerMarginTop + headerHeight);
            headerTotalHeight = headerMarginTop + headerHeight + headerMarginBottom;
        }

        // 布局卡片view
        int size = viewList.size();
        Log.e(TAG, "childsize=" + size);
        int childMaxHeight = 0;
        for (int i = 0; i < size; i++) {
            View viewItem = viewList.get(i);
            int childHeight = viewItem.getMeasuredHeight();
            childMaxHeight = Math.max(childMaxHeight, childHeight + itemMarginTop);
            int viewLeft = (getWidth() - viewItem.getMeasuredWidth()) / 2;
            viewItem.layout(viewLeft,
                    headerTotalHeight + itemMarginTop,
                    viewLeft + viewItem.getMeasuredWidth(),
                    headerTotalHeight + itemMarginTop + childHeight);
            int offset = yOffsetStep * i;
            float scale = 1 - SCALE_STEP * i;
            if (i > 2) {
                // 备用的view
                offset = yOffsetStep * 2;
                scale = 1 - SCALE_STEP * 2;
            }

            viewItem.offsetTopAndBottom(offset);
            viewItem.setScaleX(scale);
            viewItem.setScaleY(scale);
        }

        // 布局footer view
        if(null != footerLayout && size > 0) {
            int layoutTop = viewList.get(0).getBottom() + bottomMarginTop;
            footerLayout.layout(left, layoutTop, right, layoutTop
                    + footerLayout.getMeasuredHeight());
        }

        // 初始化一些中间参数
        if(mAdapter != null && size > 0) {
            initCenterViewX = viewList.get(0).getLeft();
            initCenterViewY = viewList.get(0).getTop();
            childWith = viewList.get(0).getMeasuredWidth();
        }
    }

    /**
     * 设置卡片操作回调
     */
    public void setCardSwitchListener(CardSwitchListener cardSwitchListener) {
        this.cardSwitchListener = cardSwitchListener;
    }

    /**
     * 卡片回调接口
     */
    public interface CardSwitchListener {
        /**
         * 新卡片显示回调
         *
         * @param index 最顶层显示的卡片的index
         */
        public void onShow(int index);

        /**
         * 卡片飞向两侧回调
         *
         * @param index 飞向两侧的卡片数据index
         * @param type  飞向哪一侧{@link #VANISH_TYPE_LEFT}或{@link #VANISH_TYPE_RIGHT}
         */
        public void onCardVanish(int index, int type);

        /**
         * 卡片点击事件
         *
         * @param cardImageView 卡片上的图片view
         * @param index         点击到的index
         */
        public void onItemClick(View cardImageView, int index);
    }
}