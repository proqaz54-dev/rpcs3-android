package net.rpcs3.android;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

public class MainActivity extends Activity implements SurfaceHolder.Callback
{
	private static final String TAG = "RPCS3";

	private SurfaceView surfaceView;

	private native int nativeBoot(String bootPath);

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		surfaceView = new SurfaceView(this);
		surfaceView.getHolder().addCallback(this);
		setContentView(new FrameLayout(this) {{
			addView(surfaceView);
		}});
		System.loadLibrary("rpcs3_android");
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder)
	{
		Log.i(TAG, "surfaceCreated");
		NativeBridge.setSurface(holder.getSurface());
		startEmulation();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
	{
		Log.i(TAG, "surfaceChanged: " + width + "x" + height);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{
		Log.i(TAG, "surfaceDestroyed");
		NativeBridge.setSurface(null);
	}

	private void startEmulation()
	{
		String bootPath = null; // TODO: file picker -> game path
		Log.i(TAG, "nativeBoot");
		nativeBoot(bootPath);
	}
}

class NativeBridge
{
	private static volatile Surface surface;

	static Surface getSurface()
	{
		return surface;
	}

	static void setSurface(Surface s)
	{
		surface = s;
	}
}