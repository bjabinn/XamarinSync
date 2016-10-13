package services;


import Models.MenuItem;
import activities.GridLayoutActivity;
import activities.HomePageActivity;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import database.BaseObject;
import gcmbroadcastreceiver.GCMSesionReceiver;
import generatedclasses.AddvMedia;
import generatedclasses.AddvSync;
import generatedclasses.AddvSyncContext;
import generatedclasses.CmsxGroup;
import helper.DateEx;
import stammheim.shopper.MainActivity;
import stammheim.shopper.MainApplication;
import businesslogic.Session;
import businesslogic.Settings;
import enums.MediaTypes;
import enums.Sycofrows;
import enums.SyncChangeRequest;
import enums.TableTypes;

import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@SuppressLint("NewApi")
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class Newsyncservice extends ServiceBase {
	
	final static int SyncSpeedFast = 1;
	final static int SyncSpeedNormal = 12;
	final static int SyncSpeedSlow = 60;
	final static String  PRELOADING_AUTOMATIC = "2";
	
	Boolean firstSyncAfterStart = true;
	Boolean initialSync = false;
	Boolean secondSync = false;
	Boolean venueSettingsChanged = false;
	Boolean menuChanged = false;
	boolean userChanged = false;
	Boolean catalogChanged = false;
	Boolean categoryChanged = false;
	Boolean basketChanged = false;
	Boolean positionsModified = false;
	
	public static boolean syncIsActive = false;
	public static boolean isLoadElemnts = false;
	public static boolean secondSyncDone = false;
	
	final static int SyncStateNormalSync = 0;
	final static int SyncStateInitialSyncDone = 1;
	final static int SyncStateSecondSyncDone = 2;
	final static int SyncStateMediasLeft = 3; 
	int syncState = SyncStateNormalSync;
	
	int syncSpeed = SyncSpeedNormal;
	
	int failedSyncs = 0;
	int failedTransmits = 0;
	Boolean hasTransmits = false;
	Boolean doReload = false;
	int widthDisplay,heightDisplay;
	List<Long> changedBaskets = new ArrayList<Long>();

	int cnt = 0;
	int tfx = 0;
	int numTotalMedias = 0;
	int numDownload = 0;
	int categorypicker; 
	int Basket=0;
	int time=12000;
	long synctrue=1;
	long syncfalse=0;

	private Newsyncservice(Context context) {
		this.context = context;
	}

	private static Newsyncservice _instance;
	public synchronized static Newsyncservice getInstance(Context context) {
		if (_instance == null) {
			_instance = new Newsyncservice(context);
		}
		return _instance;
	}
	
	public Runnable StartMedias = new Runnable() 
	{
		public void run() 
		{

			if (Session.getInstance().isNetworkConnected(context) == true) {
				preloadMedias();
				mmPreloadCompleted();			
			}
		}	
	};
	
	public Runnable StartSync = new Runnable() {
		public void run() {
			
			synchronized (_instance) {
				if (syncIsActive) {
					return;
				}
				syncIsActive = true;
			}
			
			initDialogSync();
			checkIfEmptyDB();
			Newsyncservice.this.syncStateMachine();
		}
	};
	
	private void syncStateMachine()
	{
		try {
			if (firstSyncAfterStart || MainApplication.isActivityVisible()) {
				__cyclicSync();
			}
		}
		finally {
			try {
				int syncTmo = syncSpeed * 1000;
				Thread.sleep(syncTmo);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			syncStateMachine();
		}
	}
	
	private void __cyclicSync() {

		try {
			if(Session.getInstance().maysync() == false) {
				return;
			} 
			if (Session.getInstance().isNetworkConnected(context) == false) {
				Session.getInstance().stopsync();
				Session.getInstance().SentSyncStateActivity(Sycofrows.fromInt(Sycofrows.Stoped.getInt()));
				return;
			}
			__syncNow(false);
		}
		finally {
			cyclicUpdateTasks();
		}
	}
	
	public void initDialogSync()
	{
		if(HomePageActivity.Activity!=null){
			Message msg4 = new Message();
			msg4.what = HomePageActivity.ACTION_SHOW_DIALOG;
			MainApplication.getEventBus().post(new SyncServiceEvent(msg4));
		}
	}
	
	private void checkIfEmptyDB() {
		List<AddvSyncContext> listsync=AddvSyncContext.All();
		if (listsync != null && !listsync.isEmpty()) 
		{
			isLoadElemnts = true;
			secondSyncDone = true;
			Message msg4 = new Message();
			msg4.what = HomePageActivity.INIT_SPLASH_WAIT;
			MainApplication.getEventBus().post(new SyncServiceEvent(msg4));
		}
		
	}

	public void startCategoryMapService() {
		this.context.startService(new Intent(context,CategoryMapService.class));
    }
	
	private void __syncNow(Boolean forced) {
		
		try {
			
			switch(syncState) {
			case SyncStateNormalSync:
				this.syncSpeed = SyncSpeedFast;
				break;
			 case SyncStateInitialSyncDone:
				secondSync = true;
				break;
			case SyncStateSecondSyncDone:
				this.syncSpeed = SyncSpeedNormal;
				String mmLoadingMode = Session.getInstance().Get_mmLoadingMode();
				if(mmLoadingMode.equals(PRELOADING_AUTOMATIC))
				{
					this.preloadMedias();
					this.mmPreloadCompleted();
				}
				syncState = SyncStateNormalSync;
				break;
				
			case SyncStateMediasLeft:
				String mmLeftLoadingMode = Session.getInstance().Get_mmLoadingMode();
				if(mmLeftLoadingMode.equals(PRELOADING_AUTOMATIC))
				{
					this.preloadMedias();
					this.mmPreloadCompleted();
				}
				syncState = SyncStateNormalSync;
				break;
			}
			if (Execute(forced, false)) {
				if (initialSync) {
					syncState = SyncStateInitialSyncDone;
					if(!isLoadElemnts){
						tryToShowTheIntroScreen();
					}
				} else if (secondSync) {
					syncState = SyncStateSecondSyncDone;
				}
			}
			
			List<AddvMedia> addvMediaList = AddvMedia.Where("Blob is NULL AND ExternalUID > 0 ");
			int numMediasLeft = addvMediaList.size();
			if(!initialSync && !secondSync && numMediasLeft != 0){
				syncState = SyncStateMediasLeft;
			}
			
			synchronisationDone();
		}
		catch(Exception exc) 
		{
			Session.getInstance().SentSyncStateActivity(Sycofrows.fromInt(Sycofrows.Stoped.getInt()));
		}
		finally {
			secondSync = false;
			syncIsActive = false;
		}
	}
	
	private void notifySecondSyncDone() {
		Log.d("TEST", "Notifying second sync done!!");
		Message msg = new Message();
		msg.what = HomePageActivity.CLOSE_IMMEDIATELY;
		MainApplication.getEventBus().post(new SyncServiceEvent(msg));
	}

	long  forcedtimer=0;
	public long GETforcedtimer(){
		return forcedtimer;
	}
	public void PUTforcedtimer(long time){
		forcedtimer=time;
	}
	public Runnable Forcedsync = new Runnable() {
		public void run() {

			long time=System.currentTimeMillis();
			if(time-GETforcedtimer()<3000){
				return;
			}
			PUTforcedtimer(System.currentTimeMillis());
			__syncNow(true);
		}
	};


	public Runnable Validatebasket = new Runnable() {
		public void run() {
		}
	};
	
	private void syncFailed() {
		if (hasTransmits) {
			failedTransmits += 1;
		}
		failedSyncs += 1;
		syncSpeed = SyncSpeedSlow;
		
		if (failedSyncs >= 5) {
			doReload = true;
		}
	}
	private void syncCompleted() {
		if (hasTransmits) {
			failedTransmits = 0;
		}
		failedSyncs = 0;
	}
	private void doReload() {

		failedTransmits = 0;
		failedSyncs = 0;
		syncSpeed = SyncSpeedNormal;
		boolean toDoClearDataBase=true;
		
		if(GridLayoutActivity.Activity != null)
		{
			if(GridLayoutActivity.Activity.isActive())
			{
				toDoClearDataBase=false;
			}
		}
		
		
		if(toDoClearDataBase)
		{
			Session.getInstance().ReloadEverything();
		}
	}
	private void initialSyncCompleted() {
		
		AddvMedia objMediaZip = AddvMedia.FirstOrDefault("Type="+MediaTypes.HtmlContent.getLong());
		if(objMediaZip != null)
		{
			Settings.Put("nameFolder",objMediaZip.Code);
			Settings.Put("mediaZipUID",String.valueOf(objMediaZip.ExternalUID));
		}
		
		AddvMedia objMediaHTML = AddvMedia.FirstOrDefault("Code ='Map' AND Type="+MediaTypes.Html.getLong());
		
		if(objMediaHTML != null)
		{
			Settings.Put("madiaPathHtml",objMediaHTML.MediaPath);
			Settings.Put("mediaHtmlUID",String.valueOf(objMediaHTML.ExternalUID));
		}
		
		
	}
	
	private void secondSyncCompleted() {
		secondSyncDone = true;
		notifySecondSyncDone();
	}
	
	private void tryToShowTheIntroScreen() {
		
		Cursor c = Session.getInstance().getSplashScreen();
	    if(c.getCount() == 0){
	     Message msg4 = new Message();
	     msg4.what = HomePageActivity.CLOSE_ONLINE;
	     MainApplication.getEventBus().post(new SyncServiceEvent(msg4));
	    }else{
	     Message msg4 = new Message();
	     msg4.what = HomePageActivity.SET_SPLASH_SCREEN;
	     MainApplication.getEventBus().post(new SyncServiceEvent(msg4));
	    }
		
		
	}
	private void mmPreloadCompleted() {
		
		// Don't do anything yet... maybe later
	}
	
	public synchronized Boolean preloadMedias(){
		
		try{
			List<AddvMedia> addvMediaList = AddvMedia.Where("Blob is NULL AND ExternalUID > 0 ");
			if(addvMediaList == null){
				return false;
			}
			
			if(addvMediaList.size() == 0){
				
				Log.i("Media","Done");
				
				if( GridLayoutActivity.Activity != null && GridLayoutActivity.Activity.isActive()){
				
					Session.getInstance().stopProgrogressGrig();
					Session.getInstance().progressSyncGrid();
				
					Message msg4 = new Message();
					msg4.what = GridLayoutActivity.REFRESH_GRID;
					GridLayoutActivity.Activity.handler.sendMessage(msg4);
				}
				
				return true;
			}
			
			numDownload= numDownload + 15;
			
			if( GridLayoutActivity.Activity != null && GridLayoutActivity.Activity.isActive()){
				Session.getInstance().setStateProgressBarGrid(numDownload);
			}
			
			Log.i("Media","requesting " + addvMediaList.size() + " object");
			
			StringBuilder xmlRequest = new StringBuilder();
			
			if(constructMultimediaXML(xmlRequest, addvMediaList) == false){
				return false;
			}
			if (POSTMM(xmlRequest.toString()) == false) {
				return false;
			}
			
			return preloadMedias();
		}catch(Exception e){
			return false;
		}
	}
	
	

	public synchronized Boolean Execute(Boolean forced, Boolean basketOnly) {

		try {
			int maxSync = 800;
			
			cnt = 0;
			tfx = 0;
			
			venueSettingsChanged = false;
			menuChanged = false;
			userChanged = false;
			catalogChanged = false;
			categoryChanged = false;
			positionsModified = false;
			basketChanged = false;
			hasTransmits = false;
			doReload = false;
			

			changedBaskets.clear();

			StringBuilder xmlRequest = new StringBuilder();
			xmlRequest=constructxml(xmlRequest, forced, false, maxSync);

			if (xmlRequest == null) {
				return true;
			}
			byte[] bytes = POSTSYNC(xmlRequest.toString());///xmlRequest.toString()
			if (bytes == null) {
				if (LastTransmissionFailed) {
					syncFailed();
				}
				return false;
			}
			Document doc = getDocFromBytes(bytes);
			
			if (doc == null) {
				syncFailed();
				return false;
			}
			String tmp = null;
			cnt = ((tmp=doc.valueOf("/r/@cnt")) == null || tmp.length() == 0) ? -1 : Integer.parseInt(tmp);
			tfx = ((tmp=doc.valueOf("/r/@tfx")) == null || tmp.length() == 0) ? -1 : Integer.parseInt(tmp);

			if (cnt < 0 || tfx < 0) {
				syncFailed();
				return false;
			}
			Log.e("DAOSync","GOT "+tfx +" TFX AND "+cnt+" CNT ");

			if (parseSyncRequest(doc) == false) {
				syncFailed();
				return false;
			}

			if(cnt > 0) {
				Session.getInstance().SyncBeginMainactivity(cnt);
				if (parseSyncRequestjson(bytes,doc,tfx) == false) {
					syncFailed();
					return false;
				}
				Session.getInstance().SyncProgressMainactivity(tfx);
			}
			while (cnt > tfx) { 

				xmlRequest.setLength(0);
				xmlRequest.append("<?xml version=\"1.0\"?>")
				.append("<r c=\"" + ContinueSyncRequest + "\">")
				.append("<json>"+"true"+"</json>")
				.append("<android>"+"true"+"</android>")
				.append("<maxnodes>"+String.valueOf(maxSync)+"</maxnodes>");
				if(stu != null && ! stu.isEmpty()){
					xmlRequest.append("<stu>" + stu + "</stu>");
				}
				xmlRequest.append("</r>");

				bytes = POSTSYNC(xmlRequest.toString());///xmlRequest.toString()
				if (bytes == null) {
					if (LastTransmissionFailed) {
						syncFailed();
					}
					return false;
				}
				doc = getDocFromBytes(bytes);
				
				if (doc == null) {
					syncFailed();
					return false;
				}

				cnt = ((tmp=doc.valueOf("/r/@cnt")) == null || tmp.length() == 0) ? -1 : Integer.parseInt(tmp);
				tfx = ((tmp=doc.valueOf("/r/@tfx")) == null || tmp.length() == 0) ? -1 : Integer.parseInt(tmp);

				if (cnt < 0 || tfx < 0) {
					syncFailed();
					return false;
				}
				Log.e("DAOSync","GOT "+tfx +" TFX AND "+cnt+" CNT ");

				if (parseSyncRequestjson(bytes, doc, tfx) == false) {
					syncFailed();
					return false;
				}
				Session.getInstance().SyncProgressMainactivity(tfx);
			}
			long lsi = ((tmp=doc.valueOf("/r/@lsi")) == null || tmp.length() == 0) ? -1 : Long.parseLong(tmp);
			if (lsi < 0) {
				syncFailed();
				return false;
			}
			if (lsi == 0) {
				doReload = true;
				return false;
			}
			syncCompleted();
			
			String lsiStr = String.valueOf(lsi);
			UpdateSyncContextAndCatalogs(doc, lsiStr);

			Log.e("DAOSync","Current Sync ID is "+lsiStr);  
			Settings.Put("LastSyncUID",lsiStr);
			Session.getInstance().LastSyncUpdate = new Date();
			
			if (venueSettingsChanged && MainActivity.Activity!= null) {

				Message refreshActionBarMessage = Message.obtain();
				refreshActionBarMessage.what = MainActivity.ACTION_REFRESH_ACTIONBAR;
				MainActivity.Activity.handler.sendMessage(refreshActionBarMessage);
			}
			
			if (firstSyncAfterStart || venueSettingsChanged) {
				Session.getInstance().checkversion(context);
			}
			if (menuChanged && MainActivity.Activity != null) {
				
				Message refreshActionBarMessage = Message.obtain();
				refreshActionBarMessage.what = MainActivity.ACTION_REFRESH_ACTIONBAR;
				MainActivity.Activity.handler.sendMessage(refreshActionBarMessage);
			}
			
			if (userChanged && GridLayoutActivity.Activity != null) {
				
				Message updateBtnOwner = Message.obtain();
				updateBtnOwner.what = GridLayoutActivity.UPDATEBUTTON;
				GridLayoutActivity.Activity.handler.sendMessage(updateBtnOwner);
			}
			
			if (this.changedBaskets.size() > 0) {

			}
			
			if(initialSync){
				initialSyncCompleted();
			}
			
			if(secondSync){
				secondSyncCompleted();
			}
			
			if (catalogChanged || categoryChanged) {
				refreshBaskets();
			}
			firstSyncAfterStart = false;
			return (tfx > 0);
		} 
		catch (Exception e) {
			syncFailed();
			return false;
		} 
		finally {
			safeClose();
			Session.getInstance().SyncEnd();
			Session.getInstance().SyncEndMainactivity();
			
			if (doReload)
			{
				doReload();
			}
		}
	}

	public Boolean UpdateSyncContextAndCatalogs(Document doc, String lsi){

		StringBuilder ctxCodeList = new StringBuilder();
		StringBuilder catCodeList = new StringBuilder();

		List<? extends Node> ctxNodes = doc.selectNodes("/r/ctx");
		for (Node ctxNode : ctxNodes) {

			String ctxCode = ctxNode.valueOf("@code");
			if (ctxCode.equals("CMSGroup")) {
				List<? extends Node> catNodes = ctxNode.selectNodes("id");
				for (Node catNode : catNodes) {
					String idStr = catNode.getText().trim();
					Long id = Long.valueOf(idStr);
					if (id <= 0) {
						continue;
					}
					catCodeList.append(idStr + ",");
				}
				continue;
			}
			ctxCodeList.append("\"" + ctxCode + "\",");
		}
		ContentValues cv = new ContentValues();
		cv.put("LastSyncUID", lsi);
		cv.put("NextSync", DateEx.toDatabaseValue(DateEx.GetUTCdatetimeAsDate()));

		if (ctxCodeList.length() > 0) {
			ctxCodeList.setLength(ctxCodeList.length()-1);
			db.Connection.update("AddvSyncContext", cv, "Code IN ("+ctxCodeList.toString()+")", null);	
		}
		if (catCodeList.length() > 0) {
			catCodeList.setLength(catCodeList.length()-1);
			db.Connection.update("CmsxGroup", cv, "ExternalUID IN ("+catCodeList.toString()+")", null);	
		}
		return true;
	}
	
	
	public boolean constructMultimediaXML(StringBuilder xmlRequest, List<AddvMedia> addvMediaList)
	{
		xmlRequest.append("<r>");
		
		if(stu != null && !stu.isEmpty()){
			xmlRequest.append("<stu>" + stu + "</stu>");
		}
		
		getDisplay();
		
		xmlRequest.append("<width>" + widthDisplay + "</width>");
		xmlRequest.append("<height>" + heightDisplay + "</height>");
		
		for(AddvMedia item: addvMediaList){
			xmlRequest.append("<MediaUID>" + item.ExternalUID + "</MediaUID>");
		}
		
		xmlRequest.append("</r>");
		
		return true;
	}
	
	public void getDisplay(){
		
		Display mdisplay = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		Point size=new Point(widthDisplay, heightDisplay);
		mdisplay.getSize(size);
		widthDisplay=size.x;
		heightDisplay=size.y;
		
		if(widthDisplay > 375)
		{
			widthDisplay = 375;
		}
		
		if(heightDisplay > 640)
		{
			heightDisplay = 640;
		}
				
	}
	
	public StringBuilder constructxml(StringBuilder xmlRequest, Boolean force, Boolean basketOnly, int maxSync) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		String ver = Session.getInstance().GetVersion();
		String Build = Session.getInstance().Getbuilt();
		String androidOS = String.valueOf(Session.getAPIVerison());
		String HW = Session.getInstance().getDeviceName();

		xmlRequest .append("<r c=\"" + SyncRequest + "\">")
		.append("<json>"+"true"+"</json>");

		Boolean authenticated = false;
		if(gcm == null){
			gcm = GCMSesionReceiver.getInstance().getRegistrationId(MainApplication.ApplicationContext);
			if (gcm == null) {
				gcm = "";
			}
		}
//		String aid = AID();

		if(gcm != null && !gcm.isEmpty()){
			xmlRequest.append("<gcm>"+gcm+"</gcm>");
		}
		if(stu != null && !stu.isEmpty()){
			authenticated = true;
			xmlRequest.append("<stu>" + stu + "</stu>");
		}
//		if (aid != null && !aid.isEmpty()) {
//			xmlRequest.append("<aid>" + aid + "</aid>");
//		}
		xmlRequest.append("<ver>"+ver+"</ver>")
		.append("<hw>"+HW+"</hw>")
		.append("<build>"+Build+"</build>")
		.append("<android>"+androidOS+"</android>")
		.append("<maxnodes>"+String.valueOf(maxSync)+"</maxnodes>");
		Long lsi = Settings.GetLong("LastSyncUID");
		if (lsi > 0) {
			xmlRequest.append("<lsi>"+String.valueOf(lsi)+"</lsi>");
		}
		Boolean hasData = false;

		List<AddvSyncContext> listsync=AddvSyncContext.All();
		if (listsync == null || listsync.isEmpty()) {
			initialSync = true;
			hasData = true;
		}
		else {
			initialSync = false;

			for (AddvSyncContext addvSyncContext : listsync) {
				long currenttime =System.currentTimeMillis();
				long lasttime = Settings.GetLong(addvSyncContext.Code+"timer");
				long secondsSince = (lasttime == 0) ? 0 : ((currenttime-lasttime)/1000);

				if (addvSyncContext.ignore()) {
					continue;
				}
				if (basketOnly && ! addvSyncContext.Code.equals("UserGroupBasket")) {
					continue;
				}
				if (addvSyncContext.authenticatedOnly() && authenticated == false) {
					continue;
				}
				if(addvSyncContext.Code.equals("CMSGroup")) {
					List<CmsxGroup> cmsxgrouplist=Session.getInstance().GETCMSGROUPREQIDS();

					Boolean hasCatalogs = false;
					for (CmsxGroup cmsxGroup : cmsxgrouplist) {

						if (cmsxGroup.syncAlways() == false) {
							if (cmsxGroup.syncOnDemand() == false) {
								continue;
							}
							if (cmsxGroup.syncNow() == false) {
								continue;
							}
						}
						if (cmsxGroup.NextSync != null) {
							continue;
						}
						if(cmsxGroup.LastSyncUID > 0) {
							xmlRequest.append("<ctx ids=\""+ cmsxGroup.ExternalUID +" ,0\" lsi=\""+cmsxGroup.LastSyncUID+"\">"+ "CMSGroup" + "</ctx>");
						}
						else {
							xmlRequest.append("<ctx ids=\""+ cmsxGroup.ExternalUID +" ,0\" >"+ "CMSGroup" + "</ctx>");
						}
						Log.e("DAOSync",addvSyncContext.Code+"."+cmsxGroup.ExternalUID+" requested - SyncUID is  "+cmsxGroup.LastSyncUID);
						hasCatalogs = true;
					}
					if (hasCatalogs) {
						hasData = true;
					}
					continue;
				}
				if (force == false && secondsSince > 0 && secondsSince < addvSyncContext.Cycle) {
					continue;
				}
				Settings.Put((addvSyncContext.Code+"timer"), String.valueOf(System.currentTimeMillis()));

				if (addvSyncContext.LastSyncUID > 0) {
					xmlRequest.append("<ctx lsi=\""+addvSyncContext.LastSyncUID+"\">"+ addvSyncContext.Code + "</ctx>");
				}
				else {
					xmlRequest.append("<ctx>"+ addvSyncContext.Code + "</ctx>");
				}
				hasData = true;
				Log.e("DAOSync",addvSyncContext.Code+" requested - SyncUID is  "+addvSyncContext.LastSyncUID);
			}
		}
		List<AddvSync> syncList = AddvSync.All("SyncUID ASC");
		if (syncList != null && syncList.size() > 0) {

			BaseObject o = null;
			for (AddvSync sync : syncList) {

				String tableName = TableTypes.fromIntString(sync.TableID);
				String daoTrace = "REQ:"+tableName + "|" + sync.ExternalUID + "|" + sync.ObjectUID + "|" + sync.ChangeType;
				o = sync.GetObject();
				if (o != null) {
					daoTrace += "|" +o.toString();
				}
				if (sync.ChangeType == 2) {
					if (sync.ExternalUID <= 0) {
						AddvSync.Delete("SyncUID = " + sync.SyncUID, false);
						Log.e("DAOSync", daoTrace +": Ignored");
						continue;
					}
					String primKey = tableName.substring(4) + "UID";
					xmlRequest.append("<" + tableName + " " + primKey + "=\"" + String.valueOf(sync.ExternalUID) + "\" DAOMsg=\"2\" />");
					hasData = true;
					hasTransmits = true;
					Log.e("DAOSync", daoTrace +": Requested");
					continue;
				}
				if ((o = sync.GetObject()) == null) {
					AddvSync.Delete("SyncUID = " + sync.SyncUID, false);
					Log.e("DAOSync", daoTrace +": Ignored - object not found");
					continue;
				}
				xmlRequest.append(o.Serialize(sync.ChangeType));
				hasTransmits = true;
				hasData = true;
				Log.e("DAOSync", daoTrace +": Requested");
			}
		}
		xmlRequest.append("</r>");

		if (hasData == false) {
			return null;
		}
		return xmlRequest;
	}

	private void synchronisationDone(){

		//stopCategoryMapService();
		
	}
	private void cyclicUpdateTasks() {

		Session.getInstance().CyclicUpdateTasks();
	}
	private void refreshBaskets() {

		if (MainActivity.Activity != null) 
		{
			Message refreshActionBarMessage = Message.obtain();
			refreshActionBarMessage.what = MainActivity.ACTION_REFRESH_ACTIONBAR;
			MainActivity.Activity.handler.sendMessage(refreshActionBarMessage);
		}
	}

	public Boolean parseSyncRequest(Document doc) {

		int failed = 0;
		List<? extends Node> nodes = doc.selectNodes("//*[starts-with(name(),'DAORsp')]");
		for (Node node : nodes) {
			if (parseasy(node, doc)) {
				continue;
			}
			failed++;
		}
		if (failed == 0) {
			return true;
		}
		LogClientError("Count not process some DAORsp messages: " + failed + "/" + nodes.size());
		return false;
	}

	public Boolean parseasy(Node node,Document doc) {
		
		String daoTrace = node.getName();
		String error = null;
		try {

			String name = node.getName();

			if (name.indexOf("DAORsp") != 0) {
				error = "RSP: Got DAO response with syntax error! Dropped...";
				return false;
			}

			String daoClass = node.valueOf("@DAOClass");
			String daoObjectValue = node.valueOf("@DAOObject");
			String extDaoObjectValue = node.valueOf("@ExtDAOObject");
			String statusValue = node.valueOf("text()");
			daoTrace = "RSP:" + daoClass + "|" + daoObjectValue + "|" + extDaoObjectValue;
			
			if (daoClass == null || daoClass.isEmpty() || daoClass.length() < 5) {
				error = daoTrace + ": Got unknown DAO class as response! Dropped...";
				return false;
			}
			Long extDAOObjectUID = (extDaoObjectValue == null || extDaoObjectValue.isEmpty()) ? -1 : Long.parseLong(extDaoObjectValue); //Android Internal UID
			Long daoObjectUID = (daoObjectValue == null || daoObjectValue.isEmpty()) ? -1 : Long.parseLong(daoObjectValue); //Server Object UID
			SyncChangeRequest changeRequestStatus = (statusValue == null || statusValue.isEmpty()) ? SyncChangeRequest.Ignored : SyncChangeRequest.fromInt(Integer.parseInt(statusValue));
			String primKey = daoClass.substring(4) + "UID";
			TableTypes table = TableTypes.fromString(daoClass);
			
			if (table == null) {
				error = daoTrace + ": Got unknown table! Dropped...";
				return false;
			}
			if (extDAOObjectUID <= 0 && daoObjectUID <= 0) {
				error = daoTrace + ": Got invalid DAO response! Dropped...";
				return false;
			}
			AddvSync syncEntry = null;
			if (extDAOObjectUID > 0) {
				syncEntry = AddvSync.FirstOrDefault("ObjectUID = " + extDAOObjectUID + " AND TableID = " + table.getInt());
			}
			if (syncEntry == null && daoObjectUID > 0) {
				syncEntry = AddvSync.FirstOrDefault("ExternalUID = " + daoObjectUID + " AND TableID = " + table.getInt());
			}
			if (syncEntry == null) {
				error = daoTrace + ": Got unknown DAO response! Dropped...";
				return false;
			}
			daoTrace += "|" + syncEntry.ChangeType;

			syncEntry.Delete(false);
			BaseObject o = syncEntry.GetObject();

			if (o != null) {
				daoTrace += "|" + o.toString();
			}
			if (changeRequestStatus == SyncChangeRequest.Done) {

				if(this.positionsModified == false && (daoClass.equals("AddvPosition") || daoClass.equals("AddvBasket"))) {
					this.positionsModified = true;
				}
				if (syncEntry.ChangeType == 2) {
					if (o != null) {
						o.Delete(false);
						Log.e("DAOSync", daoTrace + ": Executed and associated object deleted");
					}
					else {
						Log.e("DAOSync", daoTrace + ": Executed");
					}
					return true;
				}
				if (o == null) {
					Log.e("DAOSync", daoTrace + ": Executed but no object found");
					return true;
				}
				ContentValues cv = new ContentValues();
				if (o.ExternalUID != daoObjectUID) {
					cv.clear();
					cv.put("ExternalUID", daoObjectUID);
					if (db.Connection.update(daoClass, cv, primKey + " = " + String.valueOf(extDAOObjectUID), null) != 1) {
						Log.e("DAOSync", daoTrace + ": Executd but could not update object!");
						return true;
					}
				}
				if (daoClass.equals("AddvMedia")) {
					cv.clear();
					cv.put("PhotoUID", daoObjectUID);
					db.Connection.update("AddvUser", cv, "PhotoUID = " + String.valueOf(extDAOObjectUID), null);
				} 
				else if(daoClass.equals("AddvBasket")) {
					cv.clear();
					cv.put("BasketUID", daoObjectUID);
					db.Connection.update("AddvPosition", cv, "BasketUID = " + String.valueOf(extDAOObjectUID), null);
				}
				Log.e("DAOSync", daoTrace + ": Executed");
				return true;
			}
			if (o != null && o.ExternalUID <= 0) {
				o.Delete(false);
				Log.e("DAOSync", daoTrace + ": Rejected and associated object deleted.");
			}
			else {
				Log.e("DAOSync", daoTrace + ": Rejected and ignored.");
			}
			return true;
		}
		catch(Exception exc) {
			error = daoTrace + ": Exception: "+exc.getMessage();
			return false;
		}
		finally {
			if (error != null && error.isEmpty() == false) {
				LogClientError(error);
				Log.e("DAOSync", error);
			}
		}
	}

	public Boolean parseSyncRequestjson(byte[] bytes, Document doc, int size) throws NoSuchMethodException {

		try {
			if (bytes == null ||  bytes.length < 4) {
				return false;
			}
			byte[] bytes2 = Arrays.copyOfRange(bytes,0,4);
			ByteBuffer buffer = ByteBuffer.wrap(bytes2);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			int result = buffer.getInt();
			
			if (bytes.length < result+4) {
				return false;
			}
			if (bytes.length == result+4) {
				return true;
			}
			byte[] bytesw = Arrays.copyOfRange(bytes,result+4,bytes.length);
			String s = null;
			try {
				s = new String(bytesw, "UTF-8");
			} catch (UnsupportedEncodingException e2) {
				LogClientError("Could not decode UTF-8 json bytes "+String.valueOf(bytes.length-result-4));
				Log.e("DAOSync", "Got invalid binary data");
				return false;
			}
			JSONObject test = null;
			try {
				test = new JSONObject(s);
			} catch (JSONException e2) {
				LogClientError("Could not create json doc "+String.valueOf(bytes.length-result-4));
				Log.e("DAOSync", "Could not parse JSON 1");
				return false;
			}
			JSONArray rows = null;	

			int countoftables=TableTypes.values().length;
			BaseObject obj = null;

			Boolean ret = true;
			for (int i = 0; i <= countoftables; i++) {
				String name1 = TableTypes.fromIntString(i);
				try {
					rows = test.getJSONArray(name1);
				} catch (JSONException e1) {
					rows = null;
				}
				if(rows == null || rows.length() == 0) {
					continue;
				}
				try {
					Class<?> mClass = Class.forName("generatedclasses." + name1);
					obj = (BaseObject) mClass.newInstance();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					continue;
				} catch (InstantiationException e) {
					e.printStackTrace();
					continue;
				} catch (IllegalAccessException e) {
					e.printStackTrace();
					continue;
				}
				if (obj == null) {
					continue;
				}
				boolean res = obj.Deserializejson(rows);
				if (res == false) {
					LogClientError("Could not deserialize rows in " + name1);
					ret = false;
					continue;
				}
				if(name1 == "AddvVenueSetting"){
					this.venueSettingsChanged = true;
				}
				else if(name1 == "AddvMenu"){
					this.menuChanged = true;
				}
				else if(name1 == "AddvCategory"){
					this.categoryChanged = true;
				}
				else if(name1 == "CmsxObject" || name1 == "CmsxObjectCategory"){
					this.catalogChanged = true;
					
				}else if(name1 == "AddvUser"){
					this.userChanged = true;
					
				}
			}
			return ret;
		} catch (Exception exc) {
			String error = "Could not process UTF-8 json: " + exc.getMessage();
			LogClientError(error);
			Log.e("DAOSync",error);
			return false;
		}
	}
	
	
	

	

	private Document getDocFromBytes(byte[] bytes) {

		try {
			if (bytes == null || bytes.length < 4) {
				String error = "Could not create doc from server response. Got " + bytes.length + " of 4 Bytes";
				LogClientError(error);
				Log.e("DAOSync", error);
				return null;
			}
			byte[] bytescon2 = Arrays.copyOfRange(bytes, 0, 4);
			ByteBuffer buffercon = ByteBuffer.wrap(bytescon2);
			buffercon.order(ByteOrder.LITTLE_ENDIAN);
			int resultcon = buffercon.getInt();

			if (bytes.length < resultcon + 4) {
				String error = "Could not create doc from server response. Got " + bytes.length + " of " + (4 + resultcon) + " Bytes";
				LogClientError(error);
				Log.e("DAOSync", error);
				return null;
			}
			byte[] bytescon3 = Arrays.copyOfRange(bytes, 4, resultcon + 4);
			InputStream iscon3 = new ByteArrayInputStream(bytescon3);
			SAXReader reader1 = new SAXReader();
			Document doc1 = reader1.read(iscon3);
			return doc1;
		} catch (Exception e) {
			String error = "Could not create doc from server response " + e.getMessage();
			LogClientError(error);
			Log.e("DAOSync", error);
			return null;
		}
	}

	public Float getprice(String text){
		int start = text.indexOf("<Price_KVP>");
		int Endpoint = text.indexOf("</Price_KVP>");
		int length = "<Price_KVP>".length();
		int Startpoint=start+length;
		String prc;

		if(Endpoint!=-1){
			prc=text.substring(Startpoint, Endpoint);}
		else{

			prc="0";
		}
		return Float.parseFloat(prc);
	}
}