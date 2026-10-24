package com.oldschoolnetwork.core;

import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class CropActivity extends AppCompatActivity {

    public static final int REQUEST_GALLERY_INNER = 101;
    public static final int REQUEST_CAMERA_INNER = 102;

    private CropView cropView;
    private Bitmap currentBitmap;
    private Uri innerCameraUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        FrameLayout container = findViewById(R.id.crop_view_container);
        cropView = new CropView(this);
        container.addView(cropView);

        findViewById(R.id.btn_close_crop).setOnClickListener(v -> finish());

        findViewById(R.id.btn_select_another).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_GALLERY_INNER);
        });

        findViewById(R.id.btn_camera).setOnClickListener(v -> {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "New Avatar");
            values.put(MediaStore.Images.Media.DESCRIPTION, "From ON Camera");
            innerCameraUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, innerCameraUri);
            startActivityForResult(intent, REQUEST_CAMERA_INNER);
        });

        findViewById(R.id.btn_keep).setOnClickListener(v -> saveCrop());

        String uriStr = getIntent().getStringExtra("image_uri");
        if (uriStr != null) {
            loadBitmap(Uri.parse(uriStr));
        }
    }

    private void loadBitmap(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2; // Защита от OutOfMemory на тяжелых фотках
            currentBitmap = BitmapFactory.decodeStream(is, null, options);
            cropView.setBitmap(currentBitmap);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_GALLERY_INNER && data != null && data.getData() != null) {
                loadBitmap(data.getData());
            } else if (requestCode == REQUEST_CAMERA_INNER) {
                if (innerCameraUri != null) {
                    loadBitmap(innerCameraUri);
                }
            }
        }
    }

    private void saveCrop() {
        if (currentBitmap == null) return;

        // Защита от краша при сохранении
        Bitmap cropped = cropView.getCroppedBitmap();
        if (cropped == null) {
            Toast.makeText(this, "Crop error. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        cropped.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        String base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

        Intent result = new Intent();
        result.putExtra("cropped_base64", base64);
        setResult(RESULT_OK, result);
        finish();
    }

    // ==========================================
    // ПРОФЕССИОНАЛЬНЫЙ ВИДЖЕТ ОБРЕЗКИ
    // ==========================================
    private class CropView extends View {
        private Bitmap bitmap;
        private RectF imageRect = new RectF();
        private RectF cropRect = new RectF();

        private Paint dimPaint, borderPaint, cornerPaint;
        private int activeCorner = -1; // 2=Resize, 4=Move
        private float lastX, lastY;

        private ScaleGestureDetector mScaleDetector;
        private float mScaleFactor = 1.0f;
        private float mPosX = 0f;
        private float mPosY = 0f;

        public CropView(Context context) {
            super(context);

            dimPaint = new Paint();
            dimPaint.setColor(Color.parseColor("#99000000"));

            borderPaint = new Paint();
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setColor(Color.parseColor("#00BCD4"));
            borderPaint.setStrokeWidth(4);

            cornerPaint = new Paint();
            cornerPaint.setColor(Color.parseColor("#00BCD4"));

            mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        }

        public void setBitmap(Bitmap bmp) {
            this.bitmap = bmp;
            mScaleFactor = 1.0f;
            mPosX = 0f;
            mPosY = 0f;
            requestLayout();
            invalidate();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (bitmap != null) setupRects(w, h);
        }

        private void setupRects(int w, int h) {
            float scale = Math.min((float) w / bitmap.getWidth(), (float) h / bitmap.getHeight());
            float imgW = bitmap.getWidth() * scale;
            float imgH = bitmap.getHeight() * scale;
            float left = (w - imgW) / 2;
            float top = (h - imgH) / 2;
            imageRect.set(left, top, left + imgW, top + imgH);

            float cropSize = Math.min(imgW, imgH);
            float cx = imageRect.centerX();
            float cy = imageRect.centerY();
            cropRect.set(cx - cropSize/2, cy - cropSize/2, cx + cropSize/2, cy + cropSize/2);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (bitmap == null) return;

            canvas.save();
            canvas.translate(mPosX, mPosY);
            canvas.scale(mScaleFactor, mScaleFactor, getWidth() / 2f, getHeight() / 2f);

            // Отрисовка фотки
            canvas.drawBitmap(bitmap, null, imageRect, null);

            // Затемнение
            canvas.drawRect(imageRect.left, imageRect.top, imageRect.right, cropRect.top, dimPaint);
            canvas.drawRect(imageRect.left, cropRect.bottom, imageRect.right, imageRect.bottom, dimPaint);
            canvas.drawRect(imageRect.left, cropRect.top, cropRect.left, cropRect.bottom, dimPaint);
            canvas.drawRect(cropRect.right, cropRect.top, imageRect.right, cropRect.bottom, dimPaint);

            // Рамка и уголки
            canvas.drawRect(cropRect, borderPaint);
            float cw = 40;
            float pw = 8;
            canvas.drawRect(cropRect.left - pw/2, cropRect.top - pw/2, cropRect.left + cw, cropRect.top + pw/2, cornerPaint);
            canvas.drawRect(cropRect.left - pw/2, cropRect.top - pw/2, cropRect.left + pw/2, cropRect.top + cw, cornerPaint);
            canvas.drawRect(cropRect.right - cw, cropRect.top - pw/2, cropRect.right + pw/2, cropRect.top + pw/2, cornerPaint);
            canvas.drawRect(cropRect.right - pw/2, cropRect.top - pw/2, cropRect.right + pw/2, cropRect.top + cw, cornerPaint);
            canvas.drawRect(cropRect.left - pw/2, cropRect.bottom - pw/2, cropRect.left + cw, cropRect.bottom + pw/2, cornerPaint);
            canvas.drawRect(cropRect.left - pw/2, cropRect.bottom - cw, cropRect.left + pw/2, cropRect.bottom + pw/2, cornerPaint);
            canvas.drawRect(cropRect.right - cw, cropRect.bottom - pw/2, cropRect.right + pw/2, cropRect.bottom + pw/2, cornerPaint);
            canvas.drawRect(cropRect.right - pw/2, cropRect.bottom - cw, cropRect.right + pw/2, cropRect.bottom + pw/2, cornerPaint);

            canvas.restore();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (bitmap == null) return false;

            mScaleDetector.onTouchEvent(event);
            if (event.getPointerCount() > 1) {
                activeCorner = -1;
                return true;
            }

            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float x = (event.getX() - mPosX - cx) / mScaleFactor + cx;
            float y = (event.getY() - mPosY - cy) / mScaleFactor + cy;

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = x; lastY = y;
                    if (cropRect.contains(x, y)) {
                        activeCorner = 4; // Перемещение
                    } else {
                        activeCorner = 2; // Ресайз
                    }
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (activeCorner != -1) {
                        if (activeCorner == 4) {
                            // Логика перемещения рамки
                            float deltaX = x - lastX;
                            float deltaY = y - lastY;
                            cropRect.offset(deltaX, deltaY);

                            // Жесткое удержание внутри фотки
                            if (cropRect.left < imageRect.left) cropRect.offset(imageRect.left - cropRect.left, 0);
                            if (cropRect.top < imageRect.top) cropRect.offset(0, imageRect.top - cropRect.top);
                            if (cropRect.right > imageRect.right) cropRect.offset(imageRect.right - cropRect.right, 0);
                            if (cropRect.bottom > imageRect.bottom) cropRect.offset(0, imageRect.bottom - cropRect.bottom);
                        } else {
                            // НОВАЯ Логика изменения размера (без инверсии)
                            float center_x = cropRect.centerX();
                            float center_y = cropRect.centerY();
                            // Вычисляем радиус до пальца
                            float halfSize = Math.max(Math.abs(x - center_x), Math.abs(y - center_y));

                            if (halfSize < 50) halfSize = 50; // Мин. размер

                            // Жесткий ограничитель: рамка не имеет права стать больше, чем расстояние от её центра до краев фотки
                            float maxHalfLeft = center_x - imageRect.left;
                            float maxHalfRight = imageRect.right - center_x;
                            float maxHalfTop = center_y - imageRect.top;
                            float maxHalfBottom = imageRect.bottom - center_y;
                            float maxAllowed = Math.min(Math.min(maxHalfLeft, maxHalfRight), Math.min(maxHalfTop, maxHalfBottom));

                            if (halfSize > maxAllowed) halfSize = maxAllowed;

                            cropRect.set(center_x - halfSize, center_y - halfSize, center_x + halfSize, center_y + halfSize);
                        }
                        lastX = x; lastY = y;
                        invalidate();
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    activeCorner = -1;
                    snapBack(); // Анимация автовозврата
                    return true;
            }
            return super.onTouchEvent(event);
        }

        // Пружинный автовозврат (Snap-Back)
        private void snapBack() {
            float targetScale = mScaleFactor;
            float targetX = mPosX;
            float targetY = mPosY;

            if (targetScale < 1.0f) {
                // Возврат масштаба на 100% и центрирование
                targetScale = 1.0f;
                targetX = 0f;
                targetY = 0f;
            } else {
                // Если приблизили, не даем увести фотку полностью за экран
                float maxTx = (imageRect.width() * targetScale) / 2f;
                float maxTy = (imageRect.height() * targetScale) / 2f;
                if (targetX > maxTx) targetX = maxTx;
                if (targetX < -maxTx) targetX = -maxTx;
                if (targetY > maxTy) targetY = maxTy;
                if (targetY < -maxTy) targetY = -maxTy;
            }

            // Запускаем плавную анимацию, если что-то сдвинулось
            if (mScaleFactor != targetScale || mPosX != targetX || mPosY != targetY) {
                PropertyValuesHolder px = PropertyValuesHolder.ofFloat("x", mPosX, targetX);
                PropertyValuesHolder py = PropertyValuesHolder.ofFloat("y", mPosY, targetY);
                PropertyValuesHolder ps = PropertyValuesHolder.ofFloat("scale", mScaleFactor, targetScale);

                ValueAnimator anim = ValueAnimator.ofPropertyValuesHolder(px, py, ps);
                anim.setDuration(300);
                anim.setInterpolator(new DecelerateInterpolator());
                anim.addUpdateListener(animation -> {
                    mPosX = (float) animation.getAnimatedValue("x");
                    mPosY = (float) animation.getAnimatedValue("y");
                    mScaleFactor = (float) animation.getAnimatedValue("scale");
                    invalidate();
                });
                anim.start();
            }
        }

        private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
            private float lastFocusX;
            private float lastFocusY;

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                lastFocusX = detector.getFocusX();
                lastFocusY = detector.getFocusY();
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                mScaleFactor *= detector.getScaleFactor();
                mScaleFactor = Math.max(0.3f, Math.min(mScaleFactor, 10.0f)); // Позволяем отдалиться до 30%, чтобы сработала пружина

                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();
                mPosX += focusX - lastFocusX;
                mPosY += focusY - lastFocusY;

                lastFocusX = focusX;
                lastFocusY = focusY;

                invalidate();
                return true;
            }
        }

        public Bitmap getCroppedBitmap() {
            // Защита от нулевых размеров
            if (cropRect.width() <= 0 || cropRect.height() <= 0) return null;

            float scaleX = bitmap.getWidth() / imageRect.width();
            float scaleY = bitmap.getHeight() / imageRect.height();

            int cX = (int) ((cropRect.left - imageRect.left) * scaleX);
            int cY = (int) ((cropRect.top - imageRect.top) * scaleY);
            int cW = (int) (cropRect.width() * scaleX);
            int cH = (int) (cropRect.height() * scaleY);

            // Финальная жесткая обрезка по границам исходного битмапа (защита от краша)
            cX = Math.max(0, cX);
            cY = Math.max(0, cY);
            if (cX + cW > bitmap.getWidth()) cW = bitmap.getWidth() - cX;
            if (cY + cH > bitmap.getHeight()) cH = bitmap.getHeight() - cY;

            if (cW <= 0 || cH <= 0) return null;

            return Bitmap.createBitmap(bitmap, cX, cY, cW, cH);
        }
    }
}