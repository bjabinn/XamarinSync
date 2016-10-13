using System;
using System.Collections.Generic;
using System.Text;
using System.Net.Http;
using System.IO;
using System.IO.Compression;


namespace Shared
{
    public class SyncService
    {
        static string DefaultUrl = "http://rabattz.de"; //Settings.GetString("Defaulurl");
        string Url = DefaultUrl + "/service/IOS3.aspx";
        string mmUrl = DefaultUrl + "/service/mm.aspx";

        public bool LastTransmissionFailed = false;

        public string ConstructXml(StringBuilder xmlRequest, bool force, bool basketOnly, int maxSync)
        {
            return "<r c=\"147\"><json>true</json><ver>2.14.00</ver><hw>asus Nexus 7</hw><build></build><android>6.0</android><maxnodes>800</maxnodes></r>";
        }

        public byte[] PostSync(string xml)
        {
            return __POST(Url, xml, true, false);
        }
        
        private async byte[] __POST(string url, string xml, bool logError, bool closeConnection) {

	
			LastTransmissionFailed = false;
			if (string.IsNullOrEmpty(xml)) {
				return null;
			}
			
			var gzipXml = Compress(xml);			    
			
			if (gzipXml == null) {
				return null;
			}
			var se = new MemoryStream(gzipXml);


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

                    var response = await client.PostAsync(url, content);

                    responseString = await response.Content.ReadAsStringAsync();
                }
		        if (string.IsNullOrEmpty(responseString))
		        {
		            return null;
		        }
			
			    GZipStream stream = null;
			    try {
				    stream = new GZipStream(is, CompressionLevel.Optimal);
				    byte[] bytes = IOUtils.toByteArray(stream);
				    return bytes;
				
			    } catch (Exception e) {
				    LastTransmissionFailed = true;
				    var error = "Could not extract bytes from server response: " + e.Message;
				    return null;
			    }
			    finally {
				    if (stream != null)
				    {
				        stream.Close();
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

        public static byte[] Compress(string str)
        {
            var bytes = Encoding.UTF8.GetBytes(str);

            using (var msi = new MemoryStream(bytes))
            using (var mso = new MemoryStream())
            {
                using (var gs = new GZipStream(mso, CompressionMode.Compress))
                {                    
                    CopyTo(msi, gs);
                }

                return mso.ToArray();
            }
        }

        public static string Decompress(byte[] bytes)
        {
            using (var msi = new MemoryStream(bytes))
            using (var mso = new MemoryStream())
            {
                using (var gs = new GZipStream(msi, CompressionMode.Decompress))
                {                    
                    CopyTo(gs, mso);
                }

                return Encoding.UTF8.GetString(mso.ToArray());
            }
        }

        public static void CopyTo(Stream src, Stream dest)
        {
            byte[] bytes = new byte[4096];

            int cnt;

            while ((cnt = src.Read(bytes, 0, bytes.Length)) != 0)
            {
                dest.Write(bytes, 0, cnt);
            }
        }




    }
}
