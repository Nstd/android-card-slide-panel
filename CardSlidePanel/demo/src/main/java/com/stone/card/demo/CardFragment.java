package com.stone.card.demo;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.stone.card.CardSlidePanel;

/**
 * 卡片Fragment
 *
 * @author xmuSistone
 */
@SuppressLint({"HandlerLeak", "NewApi", "InflateParams"})
public class CardFragment extends Fragment {

    private CardSlidePanel.CardSwitchListener cardSwitchListener;
    private View leftBtn;
    private View rightBtn;
    private CardSlidePanel slidePanel;

    private String imagePaths[] = {"assets://wall01.jpg",
            "assets://wall02.jpg", "assets://wall03.jpg",
            "assets://wall04.jpg", "assets://wall05.jpg",
            "assets://wall06.jpg", "assets://wall07.jpg",
            "assets://wall08.jpg", "assets://wall09.jpg",
            "assets://wall10.jpg", "assets://wall11.jpg",
            "assets://wall12.jpg", "assets://wall01.jpg",
            "assets://wall02.jpg", "assets://wall03.jpg",
            "assets://wall04.jpg", "assets://wall05.jpg",
            "assets://wall06.jpg", "assets://wall07.jpg",
            "assets://wall08.jpg", "assets://wall09.jpg",
            "assets://wall10.jpg", "assets://wall11.jpg", "assets://wall12.jpg"}; // 24个图片资源名称

    private String names[] = {"郭富城", "刘德华", "张学友", "李连杰", "成龙", "谢霆锋", "李易峰",
            "霍建华", "胡歌", "曾志伟", "吴孟达", "梁朝伟", "周星驰", "赵本山", "郭德纲", "周润发", "邓超",
            "王祖蓝", "王宝强", "黄晓明", "张卫健", "徐峥", "李亚鹏", "郑伊健"}; // 24个人名

    private List<CardDataItem> dataList = new ArrayList<CardDataItem>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.card_layout, null);
        initView(rootView);
        return rootView;
    }


    private View.OnClickListener btnClickListener  = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(v.getId() == R.id.card_left_btn) {
                slidePanel.vanishOnBtnClick(CardSlidePanel.VANISH_TYPE_LEFT);
            } else if(v.getId() == R.id.card_right_btn) {
                slidePanel.vanishOnBtnClick(CardSlidePanel.VANISH_TYPE_RIGHT);
            }
        }
    };

    private void initView(View rootView) {
        slidePanel = (CardSlidePanel) rootView
                .findViewById(R.id.image_slide_panel);

        leftBtn = rootView.findViewById(R.id.card_left_btn);
        rightBtn = rootView.findViewById(R.id.card_right_btn);
        leftBtn.setOnClickListener(btnClickListener);
        rightBtn.setOnClickListener(btnClickListener);

        cardSwitchListener = new CardSlidePanel.CardSwitchListener() {

            @Override
            public void onShow(int index) {
                Log.e("CardFragment", "正在显示-" + dataList.get(index).userName);
            }

            @Override
            public void onCardVanish(int index, int type) {
                Log.e("CardFragment", "正在消失-" + dataList.get(index).userName + " 消失type=" + type);
            }

            @Override
            public void onItemClick(View cardView, int index) {
                Log.e("CardFragment", "卡片点击-" + dataList.get(index).userName);
            }
        };
        prepareDataList();
        slidePanel.setCardSwitchListener(cardSwitchListener);
        ItemAdapter adapter = new ItemAdapter();
        slidePanel.setAdapter(adapter);
//        slidePanel.fillData(dataList);
    }

    class ItemAdapter extends BaseAdapter {


        @Override
        public int getCount() {
            return dataList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if(convertView == null) {
                holder = new ViewHolder();

                convertView = LayoutInflater.from(getContext()).inflate(R.layout.card_item, null);
                holder.imageView = (ImageView) convertView.findViewById(R.id.card_image_view);
                holder.maskView = convertView.findViewById(R.id.maskView);
                holder.userNameTv = (TextView) convertView.findViewById(R.id.card_user_name);
                holder.imageNumTv = (TextView) convertView.findViewById(R.id.card_pic_num);
                holder.likeNumTv = (TextView) convertView.findViewById(R.id.card_like);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            CardDataItem itemData = dataList.get(position);
            ImageLoader.getInstance().displayImage(itemData.imagePath, holder.imageView);
            holder.userNameTv.setText(itemData.userName);
            holder.imageNumTv.setText(itemData.imageNum + "");
            holder.likeNumTv.setText(itemData.likeNum + "");

            return convertView;
        }

        class ViewHolder {
            public ImageView imageView;
            public View maskView;
            public TextView userNameTv;
            public TextView imageNumTv;
            public TextView likeNumTv;
        }
    }

    private void prepareDataList() {
        int num = imagePaths.length;

        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < num; i++) {
                CardDataItem dataItem = new CardDataItem();
                dataItem.userName = names[i];
                dataItem.imagePath = imagePaths[i];
                dataItem.likeNum = (int) (Math.random() * 10);
                dataItem.imageNum = (int) (Math.random() * 6);
                dataList.add(dataItem);
            }
        }
    }

}
