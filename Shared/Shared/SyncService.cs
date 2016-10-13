using System;
using System.Collections.Generic;
using System.Text;
using System.Net.Http;


namespace Shared
{
    public class SyncService
    {
        string Defaulurl = Settings.GetString("Defaulurl");
        string Url = Defaulurl + "/service/IOS3.aspx";
        string mmUrl = Defaulurl + "/service/mm.aspx";

        string RedirectUrl = "";
        string ServiceUri = "Service/IOS3.aspx";

        public bool LastTransmissionFailed = false;





        public string ConstructXml(StringBuilder xmlRequest, bool force, bool basketOnly, int maxSync)
        {
            return "<r c=\"147\"><json>true</json><ver>2.14.00</ver><hw>asus Nexus 7</hw><build></build><android>6.0</android><maxnodes>800</maxnodes></r>";
        }

        public byte[] PostSync(string xml)
        {
            return __POST(httppostSync, xml, true, false);
        }
        
        private byte[] __POST(HttpPost httpost, string xml, bool logError, bool closeConnection) {

		    ByteArrayEntity se = null;
		
		    try {
			    LastTransmissionFailed = false;
			    if (string.IsNullOrEmpty(xml)) {
				    return null;
			    }
			
			    byte[] gzipXml = null;
			
			    gzipXml = Compress(xml);
			
			    if (gzipXml == null) {
				    return null;
			    }
			    se = new ByteArrayEntity(gzipXml);

		    } catch (Exception e) {
			    LastTransmissionFailed = true;
			    var error = "Could not prepare communicate with server: " + e.Message;
			    return null;
		    }
		    try
		    {
		        string responseString;
                using (var client = new HttpClient())
                {
                    var values = new Dictionary<string, string>
                    {
                        { "thing1", "hello" },
                        { "thing2", "world" }
                    };

                    var content = new FormUrlEncodedContent(values);

                    var response = await client.PostAsync(Url, content);

                    responseString = await response.Content.ReadAsStringAsync();
                }
		        if (string.IsNullOrEmpty(responseString))
		        {
		            return null;
		        }
			
			    GZIPInputStream stream = null;
			    try {
				    stream = new GZIPInputStream(is);
				    byte[] bytes = IOUtils.toByteArray(stream);
				    return bytes;
				
			    } catch (Exception e) {
				    LastTransmissionFailed = true;
				    var error = "Could not extract bytes from server response: " + e.Message;
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
			    var error = "Could not communicate with server: " + e.Message;
			    return null;
		    }
		    finally {
			    if (closeConnection) {
				    safeClose();
			    }
		    }
	    }


    }
}
