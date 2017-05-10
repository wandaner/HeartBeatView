package com.example.xukai.animationapplication;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by xukai on 2017/05/09.
 * 控件由两个部分组成:图片（按钮）和Text（按钮名称）
 * 控件长宽比例固定为 1.2:1
 * 按钮每次动画会闪烁两次，即放大两次
 */

public class HeartBeatView extends View {
    private Bitmap bitmap;
    private int image_id;
    private int text_id;
    private int textColor;
    private float textSize;
    //handler 三个状态
    private static final int START = 1;
    private static final int STOP = 2;
    private static final int RUNNING = 3;
    //动画需求参数
    private boolean isButtonStart = false;
    private boolean toStop = false;
//    private boolean open
    private static final int reViewTime = 2;//第一次放大周期
    private static final int UpTime1 = 200;//第一次放大周期
    private static final int UpTime2 = 100;//第二次放大周期
    private static final int DownTime1 = 100;//从第一次放大状态回归正常状态的周期
    private static final int DownTime2 = 100;//从第二次放大状态回归正常状态的周期
    private static final int gapTime = 500;//每次播放动画期间的周期
    private static final int stopTag = 20;//播放停止的标志，比gapTime小一些
    private static final int TotalTime = UpTime1+UpTime2+DownTime1+DownTime2+gapTime;//一次完整的双放大动画的总时间
    private static final float radio1 = 1.25f;//第一次放大比例
    private static final float radio2 = 1.10f;//第二次放大比例
    private static final float H2WRadio = 1.2f;//控件长宽比例
    private int firstStageEndSize;//第一次放大的最终大小
    private int secondStageEndSize;//第二次放大的最终大小
    private double firstStageUpSpeed;//第一次放大线速度（+）
    private double firstStageDownSpeed;//第一次回归原始状态线速度（-）
    private double secondStageUpSpeed;//第二次放大线速度（+）
    private double secondStageDownSpeed;//第二次回归原始状态线速度（-）
    private long startTime = 0;//记录动画开始时间
    //图片位置参数
    private boolean isFirstTime = true;//标识，所有参数只初始化一次，由于初始化参数需要用到控件的长宽，所以需要在onMeasure之后调用，故放在onDraw方法中
    private int centerX;//图片中心点X
    private int centerY;//图片中心点Y
    private int startX ;//图片绘制位置开始x
    private int startY ;//图片绘制位置开始y
    private int endX ;//图片绘制位置结束x
    private int endY ;//图片绘制位置结束y
    private int imageSize;//默认正方形，表示图片的长和宽
    private int changeableImageSize;//改变后的图片大小，表示图片的长和宽

    private Paint imagePaint;
    private Paint textPaint;

    public HeartBeatView(Context context) {
        super(context);
    }

    public HeartBeatView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context,attrs);
    }
    private void init(Context context,AttributeSet attrs){
        TypedArray typedArray = context.obtainStyledAttributes(attrs,R.styleable.sparkButton);
        image_id = typedArray.getResourceId(R.styleable.sparkButton_sparkBt_image,-1);
        text_id = typedArray.getResourceId(R.styleable.sparkButton_sparkBt_text,-1);
        textColor = typedArray.getColor(R.styleable.sparkButton_sparkBt_text_color,Color.BLACK);
        textSize = typedArray.getDimensionPixelSize(R.styleable.sparkButton_sparkBt_text_size, (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 15, getResources().getDisplayMetrics()));
        typedArray.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.makeMeasureSpec((int)(H2WRadio*width),MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, height);
    }

    private void initDate(){
        if(image_id==-1||text_id==-1){
            return;
        }
        changeableImageSize=imageSize = (int)(getWidth()*2.0/3);
        Bitmap temp = BitmapFactory.decodeResource(getResources(),image_id);
        int width = temp.getWidth();
        float ScaleXY = (float) (1.0*imageSize/width);
        Matrix matrix = new Matrix();
        matrix.setScale(ScaleXY,ScaleXY);
        bitmap = Bitmap.createBitmap(temp,0,0,width,width,matrix,true);
        temp.recycle();
        imagePaint = new Paint();
        imagePaint.setAntiAlias(true);
        imagePaint.setStyle(Paint.Style.STROKE);
        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(textColor);
        textPaint.setTextSize(textSize);
        textPaint.setTextAlign(Paint.Align.CENTER);
        //计算速度
        firstStageEndSize = (int) (imageSize*radio1);
        secondStageEndSize = (int) (imageSize*radio2);
        firstStageUpSpeed = (firstStageEndSize-imageSize)/(double)(UpTime1);
        firstStageDownSpeed = (imageSize-firstStageEndSize)/(double)(DownTime1);
        secondStageUpSpeed = (secondStageEndSize-imageSize)/(double)(UpTime2);
        secondStageDownSpeed = (imageSize-secondStageEndSize)/(double)(DownTime2);
        //图片的绘画位置
        centerX = getWidth()/2;
        centerY = (int) (centerX/1.1f);
        reSetPosition();
    }
    private void reSetPosition(){
        startX = centerX - changeableImageSize/2;
        startY = centerY -changeableImageSize/2;
        endX = centerX + changeableImageSize/2;
        endY = centerY + changeableImageSize/2;
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(isFirstTime){
            initDate();
            isFirstTime = false;
        }
        long curTime = System.currentTimeMillis();
        int pastTime = (int) ((curTime - startTime) % TotalTime);
        if (pastTime < 0) {
            return;
        } else if (pastTime <= UpTime1) {
            changeableImageSize = (int) (imageSize + pastTime * firstStageUpSpeed);
        } else if (pastTime <= (UpTime1 + DownTime1)) {
            changeableImageSize = (int) (firstStageEndSize + (pastTime - UpTime1) * firstStageDownSpeed);
        } else if (pastTime <= (UpTime1 + DownTime1 + UpTime2)) {
            changeableImageSize = (int) (imageSize + (pastTime - UpTime1 - DownTime1) * secondStageUpSpeed);
        } else if (pastTime <= UpTime1 + DownTime1 + UpTime2 + DownTime2) {
            changeableImageSize = (int) (secondStageEndSize + (pastTime - UpTime1 - DownTime1 - UpTime2) * secondStageDownSpeed);
        }else if  (pastTime <= UpTime1 + DownTime1 + UpTime2 + DownTime2+gapTime){
            changeableImageSize = imageSize;
        }
        //为了不让动画突兀结束，所以结束之前，要使一个动画周期播放结束
        if (toStop&(pastTime-(UpTime1+DownTime1+UpTime2+DownTime2))>stopTag) {
            Log.e("Log","------>pastTime and stop: "+(pastTime-(UpTime1+DownTime1+UpTime2+DownTime2)));
            mHandler.sendEmptyMessage(STOP);
        }
        reSetPosition();
        RectF targetRecF = new RectF(startX,startY,endX,endY);
        canvas.drawBitmap(bitmap,null,targetRecF,imagePaint);
        canvas.drawText(getResources().getString(text_id),getWidth()/2,getHeight()-dp2px(10),textPaint);
    }
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case START:
                    mHandler.sendEmptyMessage(RUNNING);
                    break;
                case RUNNING:
                    mHandler.sendEmptyMessageDelayed(RUNNING,reViewTime);
                    invalidate();
                    break;
                case STOP:
                    mHandler.removeMessages(RUNNING);
                    toStop = false;
                    isButtonStart = false;
//                    invalidate();
                    break;
            }
        }
    };
    private boolean isclick = false;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_UP:
                if(!isclick) {
                    isclick = true;
                    mystart();
                }else{
                    isclick =false;
                    mystop();
                }
                break;
        }

        return true;
    }
    public void mystart(){
        Log.e("Log","---------------------->start： "+isButtonStart);
        if(isButtonStart){
            return;
        }
        startTime = System.currentTimeMillis();
        isButtonStart =true;
        mHandler.sendEmptyMessage(START);
    }
    public void mystop(){
        Log.e("Log","---------------------->Stop： "+isButtonStart);
        if(!isButtonStart){
            return;
        }
        toStop = true;
    }
    private int dp2px(int dp){
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, dp, getResources().getDisplayMetrics());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        bitmap.recycle();
    }
}
