package com.ab.abflic;

import java.io.IOException;

import anywheresoftware.b4a.BA;
import anywheresoftware.b4a.BA.Author;
import anywheresoftware.b4a.BA.Events;
import anywheresoftware.b4a.BA.RaisesSynchronousEvents;
import anywheresoftware.b4a.BA.ShortName;
import anywheresoftware.b4a.BA.Version;
import io.flic.fliclib.javaclient.*;
import io.flic.fliclib.javaclient.enums.*;

@ShortName("ABFlic")
@Version(1.00f)
@Author("Alain Bailleul")
@Events(values={"Added(buttonID as String, Name as String)", "Removed(buttonID as String)", "Clicked(buttonID as String, wasQueued As boolean, timeDiff as Int)", "DoubleClicked(buttonID as String, wasQueued As boolean, timeDiff as Int)", "Holded(buttonID as String, wasQueued As boolean, timeDiff as Int)", "StatusChanged(buttonID as String, Status as Int)"})
public class ABFlic {
	private static String _eventName="";
    private static BA _ba;
    private static Object _caller;
    private String _host="localhost";
    private FlicClient client;
    private FlicClient clientWizard;
    private ScanWizard scanWizard;
    protected Thread one;
    protected Thread two;
    
    public static final int BUTTON_DISCONNECTED = 0;
	public static final int BUTTON_CONNECTION_STARTED = 1;
	public static final int BUTTON_CONNECTION_COMPLETED = 2;
    
	/*
    public static final int RESULTACTION_NONE = 0;
	public static final int RESULTACTION_UP = 1;
	public static final int RESULTACTION_DOWN = 2;
	public static final int RESULTACTION_CLICKED = 3;	
	public static final int RESULTACTION_HOLD = 4;
	public static final int RESULTACTION_SINGLECLICK = 5;
	public static final int RESULTACTION_DOUBLECLICK = 6;
	*/
     	
	public void Initialize(BA ba, String eventName, Object caller) throws IOException {
		_ba = ba;
		_eventName = eventName;
		_caller = caller;		
	}
	
	@RaisesSynchronousEvents
	public void StartListening() throws IOException {
		one = new Thread() {
		    public void run() {
		        try {
		        	client = new FlicClient(_host);	    
		   	     
		   	     	client.getInfo(new GetInfoResponseCallback() {
		   	            @Override
		   	            public void onGetInfoResponse(BluetoothControllerState bluetoothControllerState, Bdaddr myBdAddr,
		   	                                          BdAddrType myBdAddrType, int maxPendingConnections, int maxConcurrentlyConnectedButtons,
		   	                                          int currentPendingConnections, boolean currentlyNoSpaceForNewConnection, Bdaddr[] verifiedButtons) throws IOException {

		   	                for (final Bdaddr bdaddr : verifiedButtons) {
		   	                    client.addConnectionChannel(new ButtonConnectionChannel(bdaddr, buttonCallbacks));
		   	                }
		   	            }
		   	     	});	     
		   	     	
		   	     	
		   	     	client.setGeneralCallbacks(new GeneralCallbacks() {
		   	            @Override
		   	            public void onNewVerifiedButton(Bdaddr bdaddr) throws IOException {
		   	            	BA.Log("Another client added a new button: " + bdaddr + ". Now connecting to it...");
		   	                client.addConnectionChannel(new ButtonConnectionChannel(bdaddr, buttonCallbacks));
		   	            }
		   	     	});  
		   	     	
		   	     	          
		   	     	client.handleEvents();		
		        } catch(IOException v) {
		            System.out.println(v);
		        }
		    }  
		};

		one.start();		 
	}
	
	@RaisesSynchronousEvents
	public void GrabButton() throws IOException {
		two = new Thread() {
		    public void run() {
		        try {
		        	clientWizard = new FlicClient(_host);	    
		        	BA.Log("Press a new Flic button you want to pair.");
		        	scanWizard = new ScanWizard() {
		        		@Override
		        		public void onFoundPrivateButton() throws IOException {
		        			BA.Log("Found a private button. Please hold it down for 7 seconds to make it public.");
		        		}

		        		@Override
		        		public void onFoundPublicButton(Bdaddr bdaddr, String name) throws IOException {
		        			BA.Log("Found public button " + bdaddr + " (" + name + "). Now connecting...");
		        		}

		        		@Override
		        		public void onButtonConnected(Bdaddr bdaddr, String name) throws IOException {
		        			BA.Log("Connected. Now verifying and pairing...");
		        		}

		        		@RaisesSynchronousEvents
		        		@Override
		        		public void onCompleted(ScanWizardResult result, Bdaddr bdaddr, String name) throws IOException {
		        			//BA.Log("Completed with result " + result);
		        			if (result == ScanWizardResult.WizardSuccess) {
		        				BA.Log("Your new button is: " + bdaddr);	                	
		        				_ba.raiseEvent(_caller, _eventName + "_added", new Object[] {bdaddr.toString(), name});
		        			}
		        			client.close();
		        		}
		        	};
		        	clientWizard.addScanWizard(scanWizard);
		        	clientWizard.handleEvents();		
		        } catch(IOException v) {
		            System.out.println(v);
		        }
		    }  
		};

		two.start();	
	}
	
	public void StopListening() throws IOException {
		client.close();		
	}
	
	public void ForgetButton(final String buttonID) throws IOException {
		Bdaddr bdaddr = client.getBdaddr(buttonID);
		if (bdaddr!=null) {
			client.forceDisconnect(bdaddr);
		}
	}
	
	private static ButtonConnectionChannel.Callbacks buttonCallbacks = new ButtonConnectionChannel.Callbacks() {
        @Override
        public void onCreateConnectionChannelResponse(ButtonConnectionChannel channel, CreateConnectionChannelError createConnectionChannelError, ConnectionStatus connectionStatus) {
        	//BA.Log("Create response " + channel.getBdaddr() + ": " + createConnectionChannelError + ", " + connectionStatus);
        }

        @RaisesSynchronousEvents
        @Override
        public void onRemoved(ButtonConnectionChannel channel, RemovedReason removedReason) {
        	//BA.Log("Channel removed for " + channel.getBdaddr() + ": " + removedReason);
        	_ba.raiseEvent(_caller, _eventName + "_removed", new Object[] {channel.getBdaddr().toString()});
        }

        @RaisesSynchronousEvents
        @Override
        public void onConnectionStatusChanged(ButtonConnectionChannel channel, ConnectionStatus connectionStatus, DisconnectReason disconnectReason) {
        	//BA.Log("New status for " + channel.getBdaddr() + ": " + connectionStatus + (connectionStatus == ConnectionStatus.Disconnected ? ", " + disconnectReason : ""));
        	int status=0;
        	if (connectionStatus == ConnectionStatus.Disconnected) {
        		status = BUTTON_DISCONNECTED;        		
        	}
        	if (connectionStatus == ConnectionStatus.Connected) {
        		status = BUTTON_CONNECTION_STARTED;        		
        	}
        	if (connectionStatus == ConnectionStatus.Ready) {
        		status = BUTTON_CONNECTION_COMPLETED;        		
        	}
        	_ba.raiseEvent(_caller, _eventName + "_statuschanged", new Object[] {channel.getBdaddr().toString(),status});
        }

        /*
        @Override
        public void onButtonUpOrDown(ButtonConnectionChannel channel, ClickType clickType, boolean wasQueued, int timeDiff) throws IOException {
        	//BA.Log(channel.getBdaddr() + " " + (clickType == ClickType.ButtonUp ? "Up" : "Down"));
        	if (clickType == ClickType.ButtonUp) {
        		_ba.raiseEvent(_caller, _eventName + "_action", new Object[] {channel.getBdaddr().toString(),RESULTACTION_UP, wasQueued, timeDiff});
        	} else {
        		_ba.raiseEvent(_caller, _eventName + "_action", new Object[] {channel.getBdaddr().toString(),RESULTACTION_DOWN, wasQueued, timeDiff});
        	}
        }
        
        @Override
        public void onButtonClickOrHold(ButtonConnectionChannel channel, ClickType clickType, boolean wasQueued, int timeDiff) throws IOException {
        	if (clickType == ClickType.ButtonClick) {
        		_ba.raiseEvent(_caller, _eventName + "_action", new Object[] {channel.getBdaddr().toString(),RESULTACTION_CLICKED, wasQueued, timeDiff});
        	} else {
        		_ba.raiseEvent(_caller, _eventName + "_action", new Object[] {channel.getBdaddr().toString(),RESULTACTION_HOLD, wasQueued, timeDiff});
        	}
        }

        @Override
        public void onButtonSingleOrDoubleClick(ButtonConnectionChannel channel, ClickType clickType, boolean wasQueued, int timeDiff) throws IOException {
        	if (clickType == ClickType.ButtonSingleClick) {
        		_ba.raiseEvent(_caller, _eventName + "_action", new Object[] {channel.getBdaddr().toString(),RESULTACTION_SINGLECLICK, wasQueued, timeDiff});
        	} else {
        		_ba.raiseEvent(_caller, _eventName + "_action", new Object[] {channel.getBdaddr().toString(),RESULTACTION_DOUBLECLICK, wasQueued, timeDiff});
        	}
        }
        */

        @RaisesSynchronousEvents
        @Override
        public void onButtonSingleOrDoubleClickOrHold(ButtonConnectionChannel channel, ClickType clickType, boolean wasQueued, int timeDiff) throws IOException {
        	if (clickType == ClickType.ButtonSingleClick) {
        		_ba.raiseEvent(_caller, _eventName + "_clicked", new Object[] {channel.getBdaddr().toString(), wasQueued, timeDiff});
        	} else {
        		if (clickType == ClickType.ButtonDoubleClick) {
        			_ba.raiseEvent(_caller, _eventName + "_doubleclicked", new Object[] {channel.getBdaddr().toString(), wasQueued, timeDiff});
        		} else {
        			_ba.raiseEvent(_caller, _eventName + "_holded", new Object[] {channel.getBdaddr().toString(), wasQueued, timeDiff});
        		}
        	}
        }
    };
	
}
	
