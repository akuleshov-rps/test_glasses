/*
 * Copyright (c) 2017 Razeware LLC
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish, 
 * distribute, sublicense, create a derivative work, and/or sell copies of the 
 * Software in any work that is designed, intended, or marketed for pedagogical or 
 * instructional purposes related to programming, coding, application development, 
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works, 
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.raywenderlich.facespotter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.Log;

import com.raywenderlich.facespotter.ui.camera.GraphicOverlay;


class FaceGraphic extends GraphicOverlay.Graphic {

  private static final String TAG = "FaceGraphic";

  private static final float DOT_RADIUS = 3.0f;
  private static final float TEXT_OFFSET_Y = -30.0f;

  private boolean mIsFrontFacing;

  // This variable may be written to by one of many threads. By declaring it as volatile,
  // we guarantee that when we read its contents, we're reading the most recent "write"
  // by any thread.
  private volatile FaceData mFaceData;

  private Paint mHintTextPaint;
  private Paint mHintOutlinePaint;
  private Paint mEyeWhitePaint;
  private Paint mIrisPaint;
  private Paint mEyeOutlinePaint;
  private Paint mEyelidPaint;

  private Bitmap mGlassesBitmap;

  // We want each iris to move independently,
  // so each one gets its own physics engine.
  private EyePhysics mLeftPhysics = new EyePhysics();
  private EyePhysics mRightPhysics = new EyePhysics();


  FaceGraphic(GraphicOverlay overlay, Context context, boolean isFrontFacing) {
    super(overlay);
    mIsFrontFacing = isFrontFacing;
    Resources resources = context.getResources();
    initializePaints(resources);
    initializeGraphics(resources);

    mGlassesBitmap = BitmapFactory.decodeResource(context.getResources(),
            R.drawable.glasses);
  }

  private void initializeGraphics(Resources resources) {
  }

  private void initializePaints(Resources resources) {
    mHintTextPaint = new Paint();
    mHintTextPaint.setColor(resources.getColor(R.color.overlayHint));
    mHintTextPaint.setTextSize(resources.getDimension(R.dimen.textSize));

    mHintOutlinePaint = new Paint();
    mHintOutlinePaint.setColor(resources.getColor(R.color.overlayHint));
    mHintOutlinePaint.setStyle(Paint.Style.STROKE);
    mHintOutlinePaint.setStrokeWidth(resources.getDimension(R.dimen.hintStroke));

    mEyeWhitePaint = new Paint();
    mEyeWhitePaint.setColor(resources.getColor(R.color.eyeWhite));
    mEyeWhitePaint.setStyle(Paint.Style.FILL);

    mIrisPaint = new Paint();
    mIrisPaint.setColor(resources.getColor(R.color.iris));
    mIrisPaint.setStyle(Paint.Style.FILL);

    mEyeOutlinePaint = new Paint();
    mEyeOutlinePaint.setColor(resources.getColor(R.color.eyeOutline));
    mEyeOutlinePaint.setStyle(Paint.Style.STROKE);
    mEyeOutlinePaint.setStrokeWidth(resources.getDimension(R.dimen.eyeOutlineStroke));

    mEyelidPaint = new Paint();
    mEyelidPaint.setColor(resources.getColor(R.color.eyelid));
    mEyelidPaint.setStyle(Paint.Style.FILL);
  }

  /**
   *  Update the face instance based on detection from the most recent frame.
   */
  void update(FaceData faceData) {
    mFaceData = faceData;
    postInvalidate(); // Trigger a redraw of the graphic (i.e. cause draw() to be called).
  }

  @Override
  public void draw(Canvas canvas) {
    // Confirm that the face data is still available
    // before using it.
    FaceData faceData = mFaceData;
    if (faceData == null) {
      return;
    }

    PointF detectPosition = faceData.getPosition();
    PointF detectLeftEyePosition = faceData.getLeftEyePosition();
    PointF detectRightEyePosition = faceData.getRightEyePosition();
    PointF detectNoseBasePosition = faceData.getNoseBasePosition();
    PointF detectMouthLeftPosition = faceData.getMouthLeftPosition();
    PointF detectMouthBottomPosition = faceData.getMouthBottomPosition();
    PointF detectMouthRightPosition = faceData.getMouthRightPosition();
    PointF detectLeftEarPosition = faceData.getLeftEarPosition();
    PointF detectRightEarPosition = faceData.getRightEarPosition();
    {
      if ((detectPosition == null) ||
        (detectLeftEyePosition == null) ||
        (detectRightEyePosition == null) ||
        (detectNoseBasePosition == null) ||
        (detectMouthLeftPosition == null) ||
        (detectMouthBottomPosition == null) ||
        (detectMouthRightPosition == null) ||
        (detectLeftEarPosition == null) ||
        (detectRightEarPosition == null)){
        return;
      }
    }

    // If we've made it this far, it means that the face data *is* available.
    // It's time to translate camera coordinates to view coordinates.

    // Face position, dimensions, and angle
    PointF position = new PointF(translateX(detectPosition.x),
                                 translateY(detectPosition.y));
    float width = scaleX(faceData.getWidth());
    float height = scaleY(faceData.getHeight());


    // Eye coordinates
    PointF leftEyePosition = new PointF(translateX(detectLeftEyePosition.x),
            translateY(detectLeftEyePosition.y));
    PointF rightEyePosition = new PointF(translateX(detectRightEyePosition.x),
            translateY(detectRightEyePosition.y));

    // Head tilt
    float eulerY = faceData.getEulerY();
    float eulerZ = faceData.getEulerZ();

    // Calculate the distance between the eyes using Pythagoras' formula,
    // and we'll use that distance to set the size of the eyes and irises.
    final float EYE_RADIUS_PROPORTION = 0.45f;
    final float IRIS_RADIUS_PROPORTION = EYE_RADIUS_PROPORTION / 2.0f;
    float distance = (float) Math.sqrt(
            (rightEyePosition.x - leftEyePosition.x) * (rightEyePosition.x - leftEyePosition.x) +
            (rightEyePosition.y - leftEyePosition.y) * (rightEyePosition.y - leftEyePosition.y));
    float eyeRadius = EYE_RADIUS_PROPORTION * distance;
    float irisRadius = IRIS_RADIUS_PROPORTION * distance;

    // Draw Glasses.
    drawGlasses(canvas, leftEyePosition, rightEyePosition, width, eulerZ, eulerY);

  }


  private void drawGlasses(Canvas canvas,
                           PointF rightEyePosition,
                           PointF leftEyePosition,
                           float faceWidth,
                           float angle,
                           float angleY) {

    if (Math.abs(angleY) <= 20) {
      final float GLASSES_RATIO = (float)mGlassesBitmap.getHeight() / (float)mGlassesBitmap.getWidth();
      final float GLASSES_FACE_WIDTH_RATIO = (float)(1 / 5.0);

      float glassesWidthSidePadding = faceWidth * GLASSES_FACE_WIDTH_RATIO;
      float glassesTopPadding = faceWidth * (float)(1 / 10.0);


      double ac = Math.abs(rightEyePosition.y - leftEyePosition.y);
      double cb = Math.abs(rightEyePosition.x - leftEyePosition.x + + 2 * glassesWidthSidePadding);
      double glassesWidth = Math.hypot(ac, cb);
      double glassesHeight= glassesWidth * GLASSES_RATIO;

      Bitmap scaledBitmap = Bitmap.createScaledBitmap(mGlassesBitmap, (int) glassesWidth, (int) glassesHeight, true);
      Matrix matrix = new Matrix();
      matrix.reset();

      float centerX = rightEyePosition.x - (rightEyePosition.x - leftEyePosition.x) / 2f;
      float centerY = Math.max(rightEyePosition.y, leftEyePosition.y) - Math.abs(rightEyePosition.y - leftEyePosition.y) / 2f;
      matrix.setTranslate(centerX - scaledBitmap.getWidth()/2f - scaledBitmap.getWidth()/5f * (float) Math.sin(Math.toRadians(angleY)),
              (float)(centerY - glassesTopPadding * Math.cos(Math.toRadians(angle))));
      matrix.postRotate(angle, centerX, centerY);

      matrix.postSkew((float) -Math.sin(Math.toRadians(angleY/2f)), (float) -Math.sin(Math.toRadians(angleY/2f)),
              centerX, centerY);
      matrix.postScale((float) Math.cos(Math.toRadians(angleY)), (float) Math.cos(Math.toRadians(angleY)),
              centerX, centerY);

      canvas.drawBitmap(Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), new Matrix(), true), matrix, null);
    }
  }

}