package com.linuxhacker.usbhost;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.view.View;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class usbswitch extends Activity {
	private ToggleButton usbSwitch;
	private ToggleButton vbusSrc;
	private TextView usbState;
	private TextView vbusState;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        usbSwitch = (ToggleButton) findViewById(R.id.UsbMode);
        vbusSrc = (ToggleButton) findViewById(R.id.VbusSrc);
        usbState = (TextView) findViewById(R.id.UsbState);
        vbusState = (TextView) findViewById(R.id.VBUSState);
        
        usbSwitch.setChecked(isHostModeEnabled());
        refreshUsbStatusClick(null);
        vbusState.setText(readVbusStatus());
    }

    public void usbSwitchClick(View view) {
    	String mode;
    	
    	if (usbSwitch.isChecked()) {
    		mode = "host";
    	} else {
    		mode = "peripheral";
    	}
    	
    	Process p;
		try {
			p = Runtime.getRuntime().exec("/system/xbin/su");
	
			DataOutputStream dos = new DataOutputStream(p.getOutputStream());
    
			dos.writeBytes("echo " + mode + "> /sys/devices/platform/musb_hdrc/mode\n");
			dos.writeBytes("exit");
	    	dos.flush();
	    	dos.close();
	    	
	    	if (p.waitFor() != 0) {
	    		usbState.setText("Error updating host mode - exec");
	    	} else {
	    		refreshUsbStatusClick(view);
	    	}
		} catch (IOException e) {
			usbState.setText("Error updating host mode - write");
		} catch (InterruptedException e) {
			usbState.setText("Error updating host mode - interrupt");
		}
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}
		
    	usbSwitch.setChecked(isHostModeEnabled());
    }
    
    public void vbusSrcClick(View view) {
    	String mode;
    	String line = "";
    	
    	if (vbusSrc.isChecked()) {
    		mode = "external";
    	} else {
    		mode = "internal";
    	}
    	
    	Process p;
		try {
			p = Runtime.getRuntime().exec("/system/xbin/su");
	
			DataOutputStream dos = new DataOutputStream(p.getOutputStream());
    
			dos.writeBytes("echo " + mode + "> /sys/devices/platform/i2c_omap.1/i2c-1/1-0048/twl4030_usb/vbussrc\n");
			dos.writeBytes("exit");
	    	dos.flush();
	    	dos.close();
	    	
	    	if (p.waitFor() != 0) {
	    		vbusState.setText("Error updating vbus mode - exec");
	    	} else {
	    		line = readVbusStatus();
	    		vbusState.setText(line);
	    	}
		} catch (IOException e) {
			vbusState.setText("Error updating vbus mode - write");
		} catch (InterruptedException e) {
			vbusState.setText("Error updating vbus mode - interrupt");
		}
		
    	vbusSrc.setChecked(line.startsWith("forced external"));
    }
    
    public void refreshUsbStatusClick(View view) {    	
    	String line;
    		
    	line = readUsbStatus();
    	
    	if (line == null || line.isEmpty())
    		line = "Error reading status";
    	usbState.setText(line);

    	vbusState.setText(readVbusStatus());
    }
    
    private String readUsbStatus() {
    	try {
    		InputStream instream = new FileInputStream("/sys/devices/platform/musb_hdrc/mode");
    		
    		InputStreamReader inputreader = new InputStreamReader(instream);
    		BufferedReader buffreader = new BufferedReader(inputreader);
    		String line;
    		
    		line = buffreader.readLine();
    		instream.close();
    		
    		return line;
    		
    	} catch (java.io.FileNotFoundException e) {
    		return null;
    	} catch (java.io.IOException e) {
    		return null;
    	}
    }
    
    private String readVbusStatus() {
    	try {
    		InputStream instream = new FileInputStream("/sys/devices/platform/i2c_omap.1/i2c-1/1-0048/twl4030_usb/vbussrc");
    		
    		InputStreamReader inputreader = new InputStreamReader(instream);
    		BufferedReader buffreader = new BufferedReader(inputreader);
    		String line;
    		
    		line = buffreader.readLine();
    		instream.close();
    		
    		return line;
    		
    	} catch (java.io.FileNotFoundException e) {
    		return "Error opening status file";
    	} catch (java.io.IOException e) {
    		return "Error reading status file";
    	}
    }
    private boolean isHostModeEnabled() {
    	String line = readUsbStatus();
    	
    	if (line == null || line.isEmpty())
    		return false;
    	
    	if (line.startsWith("a_")) {
    		return true;
    	} else {
    		return false;
    	}
    }
}