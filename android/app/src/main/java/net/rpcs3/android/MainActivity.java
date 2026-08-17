package net.rpcs3.android;

import android.util.Log;

import org.libsdl.app.SDLActivity;

public class MainActivity extends SDLActivity
{
	private static final String TAG = "RPCS3";

	private native int nativeBoot(String bootPath);

	// All SDL and RPCS3 code lives inside librpcs3_android.so; the default
	// SDLActivity would look for separate "SDL3"/"main" libraries.
	@Override
	protected String[] getLibraries()
	{
		return new String[] { "rpcs3_android" };
	}

	// Entry point on the SDL thread. SDLActivity's default main() would
	// look up an SDL_main symbol in the shared library, which this project
	// does not provide; boot the RPCS3 core directly instead.
	@Override
	protected void main()
	{
		Log.i(TAG, "nativeBoot");
		nativeBoot(null);
	}
}