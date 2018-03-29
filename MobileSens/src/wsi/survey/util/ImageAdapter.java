package wsi.survey.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader.TileMode;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class ImageAdapter extends BaseAdapter {

	public List<Map<String, Object>> arrayList;

	int mGalleryItemBackground;
	private ImageView[] mImages;

	private Context mContext;

	public ImageAdapter(Context c, List<Map<String, Object>> arrayList) {
		this.mContext = c;

		this.arrayList = arrayList;
		mImages = new ImageView[arrayList.size()];
	}

	/** 反射倒影 */
	public boolean createReflectedImages() {
		final int reflectionGap = 4;
		int index = 0;
		for (Map<String, Object> imageId : arrayList) {
			Integer id = (Integer) imageId.get("images");
			if(id == null){
				return false;
			}
			
			Bitmap originalImage = BitmapFactory.decodeResource(mContext.getResources(), id);
			int width = originalImage.getWidth();
			int height = originalImage.getHeight();

			Matrix matrix = new Matrix();
			matrix.preScale(1, -1);

			Bitmap reflectionImage = Bitmap.createBitmap(originalImage, 0, height / 2, width, height / 2, matrix, false);
			Bitmap bitmapWithReflection = Bitmap.createBitmap(width, (height + height / 2), Config.ARGB_8888);

			Canvas canvas = new Canvas(bitmapWithReflection);
			canvas.drawBitmap(originalImage, 0, 0, null);

			Paint deafaultPaint = new Paint();
			canvas.drawRect(0, height, width, height + reflectionGap, deafaultPaint);
			canvas.drawBitmap(reflectionImage, 0, height + reflectionGap, null);

			Paint paint = new Paint();
			LinearGradient shader = new LinearGradient(0, originalImage.getHeight(), 0, bitmapWithReflection.getHeight() + reflectionGap, 0x70ffffff, 0x00ffffff, TileMode.CLAMP);

			paint.setShader(shader);
			paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));

			canvas.drawRect(0, height, width, bitmapWithReflection.getHeight() + reflectionGap, paint);

			ImageView imageView = new ImageView(mContext);
			imageView.setImageBitmap(bitmapWithReflection);
			imageView.setLayoutParams(new ImageGallery.LayoutParams(180, 240));
			// imageView.setScaleType(ScaleType.MATRIX);
			mImages[index++] = imageView;
		}
		return true;
	}

	@Override
	public int getCount() {
		return arrayList.size();
	}

	@Override
	public Object getItem(int position) {
		return mImages[position];
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return mImages[position];
	}

	public float getScale(boolean focused, int offset) {
		return Math.max(0, 1.0f / (float) Math.pow(2, Math.abs(offset)));
	}

}
