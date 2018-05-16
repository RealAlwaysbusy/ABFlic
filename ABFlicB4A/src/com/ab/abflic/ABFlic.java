package com.ab.abflic;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.HashMap;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import anywheresoftware.b4a.BA;
import anywheresoftware.b4a.BA.Author;
import anywheresoftware.b4a.BA.Events;
import anywheresoftware.b4a.BA.ShortName;
import anywheresoftware.b4a.BA.Version;
import anywheresoftware.b4a.IOnActivityResult;
import io.flic.lib.FlicAppNotInstalledException;
import io.flic.lib.FlicBroadcastReceiver;
import io.flic.lib.FlicBroadcastReceiverFlags;
//import io.flic.lib.FlicBroadcastReceiverFlags;
import io.flic.lib.FlicButton;
import io.flic.lib.FlicManager;
import io.flic.lib.FlicManagerInitializedCallback;

@ShortName("ABFlic")
@Version(1.00f)
@Author("Alain Bailleul")
@BA.ActivityObject
@Events(values={"Added(buttonID as String, Name as String)", "Removed(buttonID as String)", "Clicked(buttonID as String, wasQueued As boolean, timeDiff as Int)", "DoubleClicked(buttonID as String, wasQueued As boolean, timeDiff as Int)", "Holded(buttonID as String, wasQueued As boolean, timeDiff as Int)", "Error(err as Int)"})
public class ABFlic {
	private String _eventName="";
    private BA _ba;
    private IOnActivityResult ion;
    
    protected String _appID="";
    protected String _appSecret="";
    protected String _appName="";
    
    public static final int BUTTON_DISCONNECTED = 0;
	public static final int BUTTON_CONNECTION_STARTED = 1;
	public static final int BUTTON_CONNECTION_COMPLETED = 2;
	
	public static final int ERROR_FLICAPP_NOTINSTALLED = 1;
	public static final int ERROR_DIDNOTGRABBUTTON=2;
	
	private static final int RAISEACTION_CLICK_OR_DOUBLE_CLICK_OR_HOLD = 8;
	
	
	/*
	public static final int RAISEACTION_NONE = 0;
	public static final int RAISEACTION_UP_OR_DOWN = 1;
	public static final int RAISEACTION_CLICK_OR_HOLD = 2;
	public static final int RAISEACTION_CLICK_OR_DOUBLE_CLICK = 4;
	public static final int RAISEACTION_CLICK_OR_DOUBLE_CLICK_OR_HOLD = 8;
	public static final int RAISEACTION_ALL = 15;

	public static final int RESULTACTION_NONE = 0;
	public static final int RESULTACTION_UP = 1;
	public static final int RESULTACTION_DOWN = 2;
	public static final int RESULTACTION_CLICKED = 3;
	public static final int RESULTACTION_HOLD = 4;
	public static final int RESULTACTION_SINGLECLICK = 5;
	public static final int RESULTACTION_DOUBLECLICK = 6;
	*/
	
	protected ABBroadcastReceiver mReceiver;
	
	public void Initialize(final BA ba, String eventName, String appID, String appSecret, String appName ) {
		_eventName = eventName.toLowerCase();
		_ba = ba;
		_appID=appID;
		_appSecret=appSecret;
		_appName=appName;
		// in FlicManager added by Alain to allow button RSSI raise event 
		//FlicManager.SetABFlic(this);
		FlicManager.setAppCredentials(appID, appSecret, appName);		
	}
	
	public void GrabButton() {
		try {
			FlicManager.getInstance(_ba.context, new FlicManagerInitializedCallback() {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public void onInitialized(FlicManager manager) {
					ion = new IOnActivityResult() {		                
		                @Override
		                public void ResultArrived(final int resultCode, final Intent intent) {
		                	FlicManager.getInstance(_ba.context, new FlicManagerInitializedCallback() {
		                	    @Override
		                	    public void onInitialized(FlicManager manager) {
		                	      FlicButton button = manager.completeGrabButton(FlicManager.GRAB_BUTTON_REQUEST_CODE, resultCode, intent);
		                	      if (button != null) {
		                	    	  //button.registerListenForBroadcast(FlicBroadcastReceiverFlags.ALL);
		                	    	  button.registerListenForBroadcast(RAISEACTION_CLICK_OR_DOUBLE_CLICK_OR_HOLD | FlicBroadcastReceiverFlags.REMOVED );
		                	    	
		                	    	  //_ba.raiseEvent(this, _eventName + "_flicbuttongrabbed", new Object[] {but.getID(), true});
		                	    	  _ba.raiseEventFromDifferentThread(ABFlic.this, null, 0,_eventName + "_added", false , new Object[] {button.getButtonId(), button.getName()});
		                	    	  
		                	    	  //BA.Log("Grabbed a button");
		                	      } else {
		                	    	  //BA.Log("Did not grab any button");
		                	    	  //_ba.raiseEvent(this, _eventName + "_flicbuttongrabbed", new Object[] {"", false});
		                	    	  _ba.raiseEventFromDifferentThread(ABFlic.this, null, 0,_eventName + "_error", false , new Object[] {ERROR_DIDNOTGRABBUTTON});
		                	      }
		                	    }
		                	  });                    

		                }
		            };
		            
		            BA.SharedProcessBA sba = _ba.processBA.sharedProcessBA;
		            Field f;
					try {
						 f = sba.getClass().getDeclaredField("onActivityResultMap");
						 f.setAccessible(true);
				         if (f.get(sba) == null) {
				              f.set(sba, new HashMap<Integer, WeakReference<IOnActivityResult>>());
				         }
				         HashMap<Integer, WeakReference<IOnActivityResult>> onActivityResultMap = (HashMap<Integer, WeakReference<IOnActivityResult>>) f.get(sba);
				         onActivityResultMap.put(FlicManager.GRAB_BUTTON_REQUEST_CODE, new WeakReference(ion));
				         manager.initiateGrabButton(_ba.activity);	
					} catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
						e.printStackTrace();
					}		           	            
				}
			});
		} catch (FlicAppNotInstalledException err) {
			BA.LogError("Flic App is not installed");
			//_ba.raiseEvent(this, _eventName + "_error", new Object[] {ERROR_FLICAPP_NOTINSTALLED});
			_ba.raiseEventFromDifferentThread(ABFlic.this, null, 0,_eventName + "_error", false , new Object[] {ERROR_FLICAPP_NOTINSTALLED});
		}
	}	
	
	public void StartListening() {
		mReceiver = new ABBroadcastReceiver();
		_ba.context.registerReceiver(mReceiver, new IntentFilter("io.flic.FLICLIB_EVENT"));
	}
	
	public void StopListening() {
		try {
			_ba.context.unregisterReceiver(mReceiver);
		} catch (IllegalArgumentException e) {
		    if (e.getMessage().contains("Receiver not registered")) {
		        // Ignore this exception. This is exactly what is desired
		    } else {
		        // unexpected, re-throw
		        throw e;
		    }
		}
	}
	
	/*
	public void GetKnownButtons() {
		FlicManager.getInstance(_ba.context, new FlicManagerInitializedCallback() {
			@Override
			public void onInitialized(FlicManager manager) {
				List buts = new List();
				buts.Initialize();
				java.util.List<FlicButton> realbuts = manager.getKnownButtons();
				for (FlicButton but: realbuts) {
					ABFlicButton abbut = new ABFlicButton();
					abbut.inner = but;					
					buts.Add(abbut);
					_ba.raiseEventFromDifferentThread(ABFlic.this, null, 0,_eventName + "_knownbuttons", false , new Object[] {buts});
				}
			}
		});
		
	}
	*/
	
	/*
	public ABFlicButton GetButton(String buttonID) {
		if (buttons.containsKey(buttonID.toLowerCase())) {
			return buttons.get(buttonID.toLowerCase());
		}
		BA.LogError("No button found with ID: " + buttonID);
		return null;
	}
	*/
	
	public void ForgetButton(final String buttonID) {		
		FlicManager.getInstance(_ba.context, new FlicManagerInitializedCallback() {
			@Override
			public void onInitialized(FlicManager manager) {	
				FlicButton but = manager.getButtonByDeviceId(buttonID);
				if (but!=null) {
					manager.forgetButton(but);
				}
			}
		});			
	}
	
	/*
	@Hide
	public void RaiseRSSI(String mac, int rssi, int status) {
		 _ba.raiseEventFromDifferentThread(ABFlic.this, null, 0,_eventName + "_flicbuttonrssi", false , new Object[] {mac,rssi,status});
	}
	*/
	
	private class ABBroadcastReceiver extends FlicBroadcastReceiver {
		  @Override
		  protected void onRequestAppCredentials(Context context) {
			  //Set app credentials by calling FlicManager.setAppCredentials here
			  //BA.Log("onRequestAppCredentials");
			  FlicManager.setAppCredentials(_appID, _appSecret, _appName);
		  }
		  
		  /*
		  @Override
		  public void onButtonUpOrDown(Context context, FlicButton button, boolean wasQueued, int timeDiff, boolean isUp, boolean isDown) {
			  //BA.Log("onButtonUpOrDown");
			  if (isUp) {
				  _ba.raiseEventFromDifferentThread(ABFlic.this, null, 0,_eventName + "_flicbuttonaction", false , new Object[] {button.getButtonId(),RESULTACTION_UP,wasQueued,timeDiff});
			  } else {
				  _ba.raiseEventFromDifferentThread(ABFlic.this, null, 0,_eventName + "_flicbuttonaction", false , new Object[] {button.getButtonId(),RESULTACTION_DOWN,wasQueued,timeDiff});
			  }
		  }
		  	 
		 
		  @Override
		  public void onButtonClickOrHold(Context context, FlicButton button, boolean wasQueued, int timeDiff, boolean isClick, boolean isHold) {
			  //BA.Log("onButtonClickOrHold");
			  if (isClick) {
				  _ba.raiseEventFromDifferentThread(ABFlic.this, null, 0,_eventName + "_flicbuttonaction", false , new Object[] {button.getButtonId(),RESULTACTION_CLICKED,wasQueued,timeDiff});
			  } else {
				  _ba.raiseEventFromDifferentThread(ABFlic.this, null, 0,_eventName + "_flicbuttonaction", false , new Object[] {button.getButtonId(),RESULTACTION_HOLD,wasQueued,timeDiff});
			  }
		  }
		  
		 
		  @Override
		  public void onButtonSingleOrDoubleClick(Context context, FlicButton button, boolean wasQueued, int timeDiff, boolean isSingleClick, boolean isDoubleClick) {
			  //BA.Log("onButtonSingleOrDoubleClick");
			  if (isSingleClick) {
				  _ba.raiseEventFromDifferentThread(ABFlic.this, null, 0,_eventName + "_flicbuttonaction", false , new Object[] {button.getButtonId(),RESULTACTION_SINGLECLICK,wasQueued,timeDiff});
			  } else {
				  _ba.raiseEventFromDifferentThread(ABFlic.this, null, 0,_eventName + "_flicbuttonaction", false , new Object[] {button.getButtonId(),RESULTACTION_DOUBLECLICK,wasQueued,timeDiff});
			  }
		  }
		  */
		  
		  /**
			 * Used for the scenario where you want to listen on single click, double click and hold.
			 * Single clicks might be delayed for up to 0.5 seconds because we can't be sure if it was rather a double click or not until then.
			 *
			 * @param context The Context in which the receiver is running
			 * @param button The button
			 * @param wasQueued If the event was locally queued in the button because it was disconnected. After the connection is completed, the event will be sent with this parameter set to true.
			 * @param timeDiff If the event was queued, the timeDiff will be the number of seconds since the event happened.
			 * @param isSingleClick True if single click, else false
			 * @param isDoubleClick True if double click, else false
			 * @param isHold True if hold, else false
			 */
		  @Override
		  public void onButtonSingleOrDoubleClickOrHold(Context context, FlicButton button, boolean wasQueued, int timeDiff, boolean isSingleClick, boolean isDoubleClick, boolean isHold) {
			  //BA.Log("onButtonSingleOrDoubleClickOrHold");
			  if (isSingleClick) {
				  _ba.raiseEventFromDifferentThread(ABFlic.this, null, 0,_eventName + "_clicked", false , new Object[] {button.getButtonId(),wasQueued,timeDiff});
			  } else {
				  if (isDoubleClick) {
					  _ba.raiseEventFromDifferentThread(ABFlic.this, null, 0,_eventName + "_doubleclicked", false , new Object[] {button.getButtonId(),wasQueued,timeDiff});
				  } else {
					  _ba.raiseEventFromDifferentThread(ABFlic.this, null, 0,_eventName + "_holded", false , new Object[] {button.getButtonId(),wasQueued,timeDiff});
				  }
			  }
		  }
		  
		
		  
		  @Override
		  public void onButtonRemoved(Context context, FlicButton button) {
			  //BA.Log("onButtonRemoved");
		    // Button was removed
			  _ba.raiseEventFromDifferentThread(ABFlic.this, null, 0,_eventName + "_removed", false , new Object[] {button.getButtonId()});
		  }
		  
		 
	}
	
}
