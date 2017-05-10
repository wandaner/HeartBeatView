# HeartBeatView
##按钮，像心跳一样一闪一闪的

    第一次做gitHub，之前都是在看别人的。有点小紧张，大家多指正批评。这是一个自定义view，继承View，然后通过canvas绘制image。

    时间间隔10ms，每次绘制计算当前心跳图片的大小。

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
    
    每次计算changeableImageSize（图片的大小，demo里面是默认图片是正方形--当时没想开！！所以这个值代表的是正方形的边长）
    private void reSetPosition(){
        startX = centerX - changeableImageSize/2;
        startY = centerY -changeableImageSize/2;
        endX = centerX + changeableImageSize/2;
        endY = centerY + changeableImageSize/2;
    }
    重新计算targetRecF的范围（也就是绘制图片的位置）
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
                    break;
            }
        }
    };
    通过handler不停的给自己发信息，绘制每一帧的图片，形成动画，demo里面没有recycle handler（我的错），大家看的时候注意一下
    ![](https://github.com/wandaner/HeartBeatView/blob/branch/device-2017-05-10-172458.mp4_1494409022.gif)
