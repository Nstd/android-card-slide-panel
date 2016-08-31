package com.stone.card;

import android.view.View;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;

/**
 * Created by maoting on 2016/8/30.
 */

public class CardItemHelper {

    private Spring springX, springY;
    private CardSlidePanel parentView;
    private View root;

    public CardItemHelper(View root) {
        this.root = root;
        initSpring();
    }

    private void initSpring() {
        SpringConfig springConfig = SpringConfig.fromBouncinessAndSpeed(15, 20);
        SpringSystem mSpringSystem = SpringSystem.create();
        springX = mSpringSystem.createSpring().setSpringConfig(springConfig);
        springY = mSpringSystem.createSpring().setSpringConfig(springConfig);

        springX.addListener(new SimpleSpringListener() {
            @Override
            public void onSpringUpdate(Spring spring) {
                int xPos = (int) spring.getCurrentValue();
                setScreenX(xPos);
                parentView.onViewPosChanged(root);
            }
        });

        springY.addListener(new SimpleSpringListener() {
            @Override
            public void onSpringUpdate(Spring spring) {
                int yPos = (int) spring.getCurrentValue();
                setScreenY(yPos);
                parentView.onViewPosChanged(root);
            }
        });
    }

    public View getView() {
        return root;
    }


    /**
     * 动画移动到某个位置
     */
    public void animTo(int xPos, int yPos) {
        setCurrentSpringPos(root.getLeft(), root.getTop());
        springX.setEndValue(xPos);
        springY.setEndValue(yPos);
    }

    /**
     * 设置当前spring位置
     */
    private void setCurrentSpringPos(int xPos, int yPos) {
        springX.setCurrentValue(xPos);
        springY.setCurrentValue(yPos);
    }

    public void setScreenX(int screenX) {
        root.offsetLeftAndRight(screenX - root.getLeft());
    }

    public void setScreenY(int screenY) {
        root.offsetTopAndBottom(screenY - root.getTop());
    }

    public void setParentView(CardSlidePanel parentView) {
        this.parentView = parentView;
    }

    public void onStartDragging() {
        springX.setAtRest();
        springY.setAtRest();
    }

//    @Override
//    public boolean dispatchTouchEvent(MotionEvent ev) {
//        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
//            // 兼容ViewPager，触点需要按在可滑动区域才行
//            boolean shouldCapture = shouldCapture((int) ev.getX(), (int) ev.getY());
//            if (shouldCapture) {
//                parentView.getParent().requestDisallowInterceptTouchEvent(true);
//            }
//        }
//        return super.dispatchTouchEvent(ev);
//    }

    /**
     * 判断(x, y)是否在可滑动的矩形区域内
     * 这个函数也被CardSlidePanel调用
     *
     * @param x 按下时的x坐标
     * @param y 按下时的y坐标
     * @return 是否在可滑动的矩形区域
     */
    public boolean shouldCapture(int x, int y) {
        int captureLeft = root.getLeft() + root.getPaddingLeft();
        int captureTop = root.getTop() + root.getPaddingTop();
        int captureRight = root.getRight() + root.getPaddingRight();
        int captureBottom = root.getBottom() + root.getPaddingBottom();

        if (x > captureLeft && x < captureRight && y > captureTop && y < captureBottom) {
            return true;
        }
        return false;
    }
}
