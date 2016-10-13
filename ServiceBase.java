package services;

import stammheim.shopper.MainActivity;
import stammheim.shopper.MainApplication;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EncodingUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import businesslogic.Session;
import businesslogic.Settings;
import businesslogic.UIHandler;
import database.DatabaseEngine;
import enums.MediaTypes;
import enums.Sycofrows;
import generatedclasses.AddvMedia;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.os.Environment;
import android.os.Message;
import android.provider.Settings.Secure;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@SuppressLint("NewApi")
public abstract class ServiceBase {
	public static String Defaulurl = Settings.GetString("Defaulurl");
	public static String Url = Defaulurl + "/service/IOS3.aspx";
	public static String mmUrl = Defaulurl + "/service/mm.aspx";

	public static String RedirectUrl = "";
	public static String ServiceUri = "Service/IOS3.aspx";
	
	protected Context context;

	public static final int TimeoutConnection = 60000;
	public static final int TimeoutSocket = 60000;

	public int BASERSP_FAILED = 0x0000;
	public int BASERSP_OK = 0x0001;
	public int BASERSP_OK_WITH_WARNINGS = 0x0002;

	public final static int BASEERR_Unavailable = 0x0001;
	public final static int BASEERR_Unknown = 0x0002;
	public final static int BASEERR_NotImplemented = 0x0003;
	public final static int BASEERR_Timeout = 0x0004;
	public final static int BASEERR_Blocked = 0x0005;
	public final static int BASEERR_InsufficientPermissions = 0x0006;
	public final static int BASEERR_Invalid = 0x0007;
	public final static int BASEERR_MissingArgument = 0x0008;
	public final static int BASEERR_NotAuthenticated = 0x0009;
	public final static int BASEERR_Expired = 0x000A;
	public final static int BASEERR_NotFound = 0x000B;
	public final static int BASEERR_AlreadyExists = 0x000C;
	public final static int BASEERR_NotAuthorized = 0x000D;
	public final static int BASEERR_NotBookedIn = 0x000E;
	public final static int BASEERR_NotAssociated = 0x000F;

	public final static int BASEERR_Warning = 0x0010;
	public final static int BASEERR_SyntaxError = 0x0011;
	public final static int BASEERR_SemanticError = 0x0012;
	public final static int BASEERR_InternalError = 0x0013;
	public final static int BASEERR_Exception = 0x0014;

	public final static int Logon = 0x0001;
	public final static int Logoff = 0x0002;
	public final static int ValidateCustomerSession = 0x0003;
	public final static int ValidateCredentials = 0x0004;
	public final static int SetUserToken = 0x0005;

	public final static int FindProfiles = 0x0010;
	public final static int CreateProfile = 0x0012;
	public final static int UpdateProfile = 0x0013;
	public final static int DeleteProfile = 0x0014;

	public final static int UploadPosition = 0x0017;
	public final static int DownloadPosition = 0x0018;

	public final static int FindBookings = 0x0020;
	public final static int CreateBooking = 0x0022;
	public final static int UpdateBooking = 0x0023;
	public final static int DeleteBooking = 0x0024;

	public final static int FindObjects = 0x0030;
	public final static int CreateObject = 0x0032;
	public final static int UpdateObject = 0x0033;
	public final static int DeleteObject = 0x0034;

	public final static int FindVenues = 0x0040;
	public final static int CreateVenue = 0x0042;
	public final static int UpdateVenue = 0x0043;
	public final static int DeleteVenue = 0x0044;

	public final static int SendRequest = 0x0090;
	public final static int ReceiveIndication = 0x0091;
	public final static int SyncRequest = 0x0093;
	public final static int RedirectQuery = 0x0094;
	public final static int ContinueSyncRequest = 0x0095;
	public final static int SentMessageText = 0x0096;
	public final static int LogClientError = 0x0097;

	public final static int Cfg_Init = 0x00100;
	public final static int Cfg_SetSelectedItem = 0x00101;
	public final static int Cfg_OrderBasket = 0x00102;

	public final static int GetPeerDevices = 0x1000;

	public final static int DTOCreate = 0x0000;
	public final static int DTOUpdate = 00001;
	public final static int DTODelete = 0x0002;
	
	public static String gcm = null;
	public static String stu = null;
	public static String uid = null;
	private static String aid = null;
	
	public Boolean LastTransmissionFailed = false;

	DatabaseEngine db = DatabaseEngine.getInstance(MainApplication.ApplicationContext);

	HttpParams httpParameters = new BasicHttpParams();
	DefaultHttpClient httpclientd = new DefaultHttpClient(httpParameters);
	HttpPost httppostSync = new HttpPost(Url);
	HttpPost httppostMM = new HttpPost(mmUrl);
	
	public synchronized String AID() {
		if (aid == null) {
			aid = Secure.getString(MainActivity.Activity.getContentResolver(), Secure.ANDROID_ID);
		}
		return aid;
	}
	
	public void SetUserToken() {
		
		try {
			String aid = AID();
			
			if (stu == null || stu.isEmpty() || aid == null || aid.isEmpty()) {
				return;
			}
			Document xml = DocumentHelper.createDocument();
			Element root = xml.addElement("r");
			root.addAttribute("c", String.valueOf(SetUserToken));
			root.addElement("stu").setText(stu);
			root.addElement("aid").setText(aid);
			String xmlRequest = xml.asXML();
			__POST(httppostSync, xmlRequest, false, false);
		} catch (Exception exc) {
			; // Ignore this!
		}
	}

	public void Logoff() {
		
		try {
			Document xml = DocumentHelper.createDocument();
			Element root = xml.addElement("r");
			root.addAttribute("c", String.valueOf(Logoff));
			if (stu != null && stu.isEmpty() == false) {
				root.addElement("stu").setText(stu);
			}
			String xmlRequest = xml.asXML();
			__POST(httppostSync, xmlRequest, false, false);
		} catch (Exception exc) {
			; // Ignore this!
		}
	}

	public void LogClientError(String message) {
		
		try {
			Document xml = DocumentHelper.createDocument();
			Element root = xml.addElement("r");
			root.addAttribute("c", String.valueOf(LogClientError));
			if (stu != null && stu.isEmpty() == false) {
				root.addElement("stu").setText(stu);
			}
			if (gcm != null && gcm.isEmpty() == false) {
				root.addElement("gcm").setText(gcm);
			}
			root.addElement("log").setText(message);
			String xmlRequest = xml.asXML();
			__POST(httppostSync, xmlRequest, false, false);
		} catch (Exception exc) {
			; // Ignore this!
		}
	}

	public byte[] POST(String xml) {
	
		return __POST(httppostSync, xml, true, true);
	}

	public byte[] POSTSYNC(String xml) {
		
		return __POST(httppostSync, xml, true, false);
	}
	
	public boolean POSTMM(String xml) {

		return __POST_GETMEDIAS(httppostMM, xml, true,true);
	}
	
	private synchronized boolean __POST_GETMEDIAS(HttpPost httpost, String xml, Boolean logError, Boolean closeConnection) {

		ByteArrayEntity se = null;
		
		try {
			LastTransmissionFailed = false;
			if (xml == null || xml.isEmpty()) {
				return false;
			}
			
			byte[] GZIPxml = null;
			
			GZIPxml = EncodingUtils.getBytes(xml, "UTF-8");
			
			if (GZIPxml == null) {
				return false;
			}
			se = new ByteArrayEntity(GZIPxml);

		} catch (Exception e) {
			LastTransmissionFailed = true;
			String error = "Could not prepare communicate with server: " + e.getMessage();
			if (logError) {
				LogClientError(error);
			}
			Log.e("DAOSync", error);
			Session.getInstance().stopsync();
			Session.getInstance().SentSyncStateActivity(Sycofrows.fromInt(Sycofrows.Stoped.getInt()));
			return false;
		}
		try {
			httpost.setEntity(se);
			HttpConnectionParams.setConnectionTimeout(httpParameters, TimeoutConnection);
			HttpConnectionParams.setSoTimeout(httpParameters, TimeoutSocket);
			HttpResponse response2 = httpclientd.execute(httpost);
			if (response2 == null) {
				return false;
			}
			InputStream is = response2.getEntity().getContent();
			if (is == null) {
				return false;
			}
			
			//byte[] bytes = IOUtils.toByteArray(is);
			//ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(bytes));
			ZipInputStream zipStream = new ZipInputStream(new BufferedInputStream(is));
			
			try {
				byte[] buffer = new byte[40960];
				ZipEntry entry = null;
		        DatabaseEngine   db = DatabaseEngine.getInstance(MainApplication.ApplicationContext);
		        
		        while ((entry = zipStream.getNextEntry()) != null)
				{
					ContentValues values = new ContentValues();
					String filename = entry.getName();
					
					if(entry.getSize() == 0){
						values.put("Blob", new byte[0]);
					}else{
						ByteArrayOutputStream baos = new ByteArrayOutputStream((int)entry.getSize());
						int count;
						while ((count = zipStream.read(buffer, 0, buffer.length)) > 0) {
							baos.write(buffer, 0, count);
						}
						baos.flush();
	
						byte[] mBuffer = baos.toByteArray();
						
						AddvMedia mediaObj = AddvMedia.FirstOrDefault("ExternalUID =" + filename);
						
						if(mediaObj.Type == MediaTypes.HtmlContent.getLong()) // ZIP
						{
							// Extract Blob ZIP to local - Application Folder 
							Session.getInstance().putByteInAppFolderFileZIP(mBuffer);
							
						}else if (mediaObj.Type == MediaTypes.Html.getLong() && mediaObj.Code.equals("Map")) // HTML
						{
							// Extract Blob HTML to local - Application Folder
							Session.getInstance().putByteInAppFolderFileHTML(mBuffer,mediaObj.MediaPath);
						}
						
						values.put("Blob", mBuffer);
						if (mBuffer.length == 0) {
							Log.i("Media", "Got an empty buffer for "+filename+". Saving anyway");
						}
						baos.close();
					}
					if (db.Connection.update("AddvMedia", values,"ExternalUID = " + filename, null) != 1) {
						Log.i("Media", "Could not save blob to " + filename);
					}
				}
				
			} finally {
				zipStream.close();
			}
			
			return true;
			
		} catch (Exception e) {
			String error = "Could not communicate with server: " + e.getMessage();
			Log.e("DAOSync", error);
			Session.getInstance().stopsync();
			Session.getInstance().SentSyncStateActivity(Sycofrows.fromInt(Sycofrows.Stoped.getInt()));
			return false;
		}
		finally {
			if (closeConnection) {
				safeClose();
			}
		}
	}
	
	private synchronized byte[] __POST(HttpPost httpost, String xml, Boolean logError, Boolean closeConnection) {

		ByteArrayEntity se = null;
		
		try {
			LastTransmissionFailed = false;
			if (xml == null || xml.isEmpty()) {
				return null;
			}
			
			byte[] GZIPxml = null;
			
			GZIPxml = Compress(xml);
			
			if (GZIPxml == null) {
				return null;
			}
			se = new ByteArrayEntity(GZIPxml);

		} catch (Exception e) {
			LastTransmissionFailed = true;
			String error = "Could not prepare communicate with server: " + e.getMessage();
			if (logError) {
				LogClientError(error);
			}
			Log.e("DAOSync", error);
			return null;
		}
		try {
			httpost.setEntity(se);
			HttpConnectionParams.setConnectionTimeout(httpParameters, TimeoutConnection);
			HttpConnectionParams.setSoTimeout(httpParameters, TimeoutSocket);
			HttpResponse response2 = httpclientd.execute(httpost);
			if (response2 == null) {
				return null;
			}
			InputStream is = response2.getEntity().getContent();
			if (is == null) {
				return null;
			}
			
			GZIPInputStream stream = null;
			try {
				stream = new GZIPInputStream(is);
				byte[] bytes = IOUtils.toByteArray(stream);
				return bytes;
				
			} catch (Exception e) {
				LastTransmissionFailed = true;
				String error = "Could not extract bytes from server response: " + e.getMessage();
				if (logError) {
					LogClientError(error);
				}
				Log.e("DAOSync", error);
				return null;
			}
			finally {
				if (stream != null) {
					stream.close();
				}
				if (is != null) {
					is.close();
				}
			}

			
		} catch (Exception e) {
			String error = "Could not communicate with server: " + e.getMessage();
			Log.e("DAOSync", error);
			return null;
		}
		finally {
			if (closeConnection) {
				safeClose();
			}
		}
	}

	protected void safeClose() {
		if (httpclientd != null && httpclientd.getConnectionManager() != null) {
			httpclientd.getConnectionManager().closeExpiredConnections();
			httpclientd.getConnectionManager().shutdown();
			httpclientd = new DefaultHttpClient(httpParameters);
		}
	}

	public static byte[] Compress(String text) {

		try {
			byte[] gzipBuff = EncodingUtils.getBytes(text, "UTF-8");

			ByteArrayOutputStream bs = new ByteArrayOutputStream();

			GZIPOutputStream gzin = new GZIPOutputStream(bs);
			gzin.write(gzipBuff);
			gzin.finish();
			bs.close();

			byte[] buffer = bs.toByteArray();
			gzin.close();
			return buffer;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void parseCommon(Document documentElement) {
		try {
			int s = 0;
			if (documentElement.valueOf("/r/@s") != "") {
				s = Integer.parseInt(documentElement.valueOf("/r/@s"));
			}
			int e = 0;
			if (documentElement.valueOf("/r/@e") != "") {
				e = Integer.parseInt(documentElement.valueOf("/r/@e"));
			}

			if (s != 1 || e > 0) {

				displayErrorMessage(e);
				return;
			}

			if (!documentElement.valueOf("/r/venue/redirect").equals("")) {
				RedirectUrl = documentElement.valueOf("/r/venue/redirect");
			}

		} catch (Exception e) {

		}
	}

	protected void displayErrorMessage(int e) {
		Message msg = new Message();
		msg.what = UIHandler.TOAST;

		if (e > 0 && e == BASEERR_AlreadyExists) {
			msg.obj = "BASEERR_AlreadyExists";
		} else if (e > 0 && e == BASEERR_Blocked) {
			msg.obj = "BASEERR_Blocked";
		} else if (e > 0 && e == BASEERR_Exception) {
			msg.obj = "BASEERR_Exception";
		} else if (e > 0 && e == BASEERR_Expired) {
			msg.obj = "BASEERR_Expired";
		} else if (e > 0 && e == BASEERR_InsufficientPermissions) {
			msg.obj = "BASEERR_InsufficientPermissions";
		} else if (e > 0 && e == BASEERR_InternalError) {
			msg.obj = "BASEERR_InternalError";
		} else if (e > 0 && e == BASEERR_Invalid) {
			msg.obj = "BASEERR_Invalid";
		} else if (e > 0 && e == BASEERR_MissingArgument) {
			msg.obj = "BASEERR_MissingArgument";
		} else if (e > 0 && e == BASEERR_NotAssociated) {
			msg.obj = "BASEERR_NotAssociated";
		} else if (e > 0 && e == BASEERR_NotAuthenticated) {
			msg.obj = "BASEERR_NotAuthenticated";
		} else if (e > 0 && e == BASEERR_NotAuthorized) {
			msg.obj = "BASEERR_NotAuthorized";
		} else if (e > 0 && e == BASEERR_NotBookedIn) {
			msg.obj = "BASEERR_NotBookedIn";
		} else if (e > 0 && e == BASEERR_NotFound) {
			msg.obj = "BASEERR_NotFound";
		} else if (e > 0 && e == BASEERR_NotImplemented) {
			msg.obj = "BASEERR_NotImplemented";
		} else if (e > 0 && e == BASEERR_SemanticError) {
			msg.obj = "BASEERR_SemanticError";
		} else if (e > 0 && e == BASEERR_SyntaxError) {
			msg.obj = "BASEERR_SyntaxError";
		} else if (e > 0 && e == BASEERR_Timeout) {
			msg.obj = "BASEERR_Timeout";
		} else if (e > 0 && e == BASEERR_Unavailable) {
			msg.obj = "BASEERR_Unavailable";
		} else if (e > 0 && e == BASEERR_Unknown) {
			msg.obj = "BASEERR_Unknown";
		} else if (e > 0 && e == BASEERR_Warning) {
			msg.obj = "BASEERR_Warning";
		}

		if (msg.obj != null) {

			// MainApplication.uiHandler.sendMessage(msg);
		}
	}

}