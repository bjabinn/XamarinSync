using System;
using System.Collections.Generic;
using System.Text;
using System.Net.Http;
using System.IO;
using System.IO.Compression;
using System.Threading.Tasks;


namespace Shared
{
    public class SyncService
    {
        static string DefaultUrl = "http://beuren.appyshopper.com"; //Settings.GetString("Defaulurl");
        string Url = DefaultUrl + "/service/IOS3.aspx";        

        public bool LastTransmissionFailed;  //=false by default

        public string ConstructXml(StringBuilder xmlRequest, bool force, bool basketOnly, int maxSync)
        {
            return "<r c=\"147\"><json>true</json><ver>2.14.00</ver><hw>asus Nexus 7</hw><build></build><android>6.0</android><maxnodes>800</maxnodes></r>";
        }

        public byte[] PostSync(string xml)
        {
            var result = __POST(Url, xml, true, false);
            return result.Result;
        }
        
        private async Task<byte[]> __POST(string url, string xml, bool logError, bool closeConnection) {
	
			LastTransmissionFailed = false;

			if (string.IsNullOrEmpty(xml)) {
				return null;
			}
			
			var gzipXml = Compress(xml);
			
			if (gzipXml == null) {
				return null;
			}

            var httpClient = new HttpClient();

		    try
		    {		        
                HttpContent content = new ByteArrayContent(gzipXml);
                
                var response = await httpClient.PostAsync(url, content);

		        var responseContent = await response.Content.ReadAsStreamAsync();

                if (responseContent == null)
		        {
		            return null;
		        }
			
			    GZipStream stream = null;
		        try
		        {                    
		            stream = new GZipStream(responseContent, CompressionMode.Decompress);

                    var streamOut = new MemoryStream();
		            stream.CopyTo(streamOut);

		            return streamOut.ToArray();

		        }
		        catch (Exception e)
		        {
		            LastTransmissionFailed = true;
		            var error = "Could not extract bytes from server response: " + e.Message;
		            return null;
		        }
		        finally
		        {                    
		            if (stream != null)
		            {                        
		                stream.Dispose();
		            }
		        }
			
		    } catch (Exception e) {
			    var error = "Could not communicate with server: " + e.Message;
			    return null;
		    }
		    finally {
                httpClient.Dispose();                
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

                //return Encoding.UTF8.GetString(mso.ToArray());
                return string.Empty;            
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
