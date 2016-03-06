package com.lzy.ui;

import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * ================================================
 * 作    者：廖子尧
 * 版    本：1.0
 * 创建日期：2016/2/29
 * 描    述：
 * 修订历史：
 * ================================================
 */
public class DragSlideLayout extends ViewGroup {

    private static final int VEL_THRESHOLD = 6000;  // 滑动速度的阈值，超过这个绝对值认为是上下
    private int DISTANCE_THRESHOLD = 60;            // 单位是dp，当上下滑动速度不够时，通过这个阈值来判定是应该粘到顶部还是底部

    private ViewDragHelper mDragHelper;
    private GestureDetectorCompat mGestureDetector;
    private View view1;
    private View view2;
    private int viewHeight;

    private OnShowNextPageListener nextPageListener; // 手指松开是否加载下一页的监听器

    public void setOnShowNextPageListener(OnShowNextPageListener nextPageListener) {
        this.nextPageListener = nextPageListener;
    }

    public interface OnShowNextPageListener {
        void onShowNextPage();
    }

    public DragSlideLayout(Context context) {
        this(context, null);
    }

    public DragSlideLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragSlideLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        DISTANCE_THRESHOLD = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DISTANCE_THRESHOLD, getResources().getDisplayMetrics());
        mDragHelper = ViewDragHelper.create(this, 10f, new DragCallBack());
        mDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_BOTTOM);
        mGestureDetector = new GestureDetectorCompat(getContext(), new YScrollDetector());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (view1 == null) view1 = getChildAt(0);
        if (view2 == null) view2 = getChildAt(1);
        view1.layout(l, t, r, b);
        view2.layout(l, t, r, b);
        viewHeight = view1.getMeasuredHeight();
        view2.offsetTopAndBottom(viewHeight);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean shouldIntercept = false;
        boolean yScroll = mGestureDetector.onTouchEvent(ev);
        try {
            shouldIntercept = mDragHelper.shouldInterceptTouchEvent(ev);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return shouldIntercept && yScroll;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            mDragHelper.processTouchEvent(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private class DragCallBack extends ViewDragHelper.Callback {
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            // 两个子View都需要跟踪，返回true
            return true;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            if (changedView == view1) view2.offsetTopAndBottom(dy);
            if (changedView == view2) view1.offsetTopAndBottom(dy);

            // 如果不重绘，拖动的时候，其他View会不显示
            ViewCompat.postInvalidateOnAnimation(DragSlideLayout.this);
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            // 这个用来控制拖拽过程中松手后，自动滑行的速度
            return child.getHeight();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            // 滑动松开后，需要向上或者乡下粘到特定的位置, 默认是粘到最顶端
            int finalTop = 0;
            if (releasedChild == view1) {
                // 拖动第一个view松手
                if (yvel < -VEL_THRESHOLD || releasedChild.getTop() < -DISTANCE_THRESHOLD) {
                    // 向上的速度足够大 或者 向上滑动的距离超过某个阈值，就滑动到顶端
                    finalTop = -viewHeight;
                    // 下一页可以初始化了
                    if (null != nextPageListener) nextPageListener.onShowNextPage();
                }
            } else {
                // 拖动第二个view松手
                if (yvel > VEL_THRESHOLD || releasedChild.getTop() > DISTANCE_THRESHOLD) {
                    // 向下滚动到初始状态
                    finalTop = viewHeight;
                }
            }
            //触发缓慢滚动
            if (mDragHelper.smoothSlideViewTo(releasedChild, 0, finalTop)) {
                ViewCompat.postInvalidateOnAnimation(DragSlideLayout.this);
            }
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            // 阻尼滑动，让滑动位移将为1/3
            return child.getTop() + dy / 3;
        }
    }

    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    private class YScrollDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
            // 垂直滑动时dy>dx，才被认定是上下拖动
            return Math.abs(dy) > Math.abs(dx);
        }
    }
}