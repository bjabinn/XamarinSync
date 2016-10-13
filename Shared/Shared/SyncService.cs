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
            return __POST(httppostSync, xml, true, false);
        }
        
        private async byte[] __POST(HttpPost httpost, string xml, bool logError, bool closeConnection) {

		    FileStream se = null;
		
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
			    se = new FileStream(gzipXml);

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

        public static void Compress(FileInfo fileToCompress)
        {
            using (FileStream originalFileStream = fileToCompress.OpenRead())
            {
                if ((File.GetAttributes(fileToCompress.FullName) & FileAttributes.Hidden) != FileAttributes.Hidden & fileToCompress.Extension != ".gz")
                {
                    using (FileStream compressedFileStream = File.Create(fileToCompress.FullName + ".gz"))
                    {
                        using (var compressionStream = new GZipStream(compressedFileStream, CompressionLevel.Fastest))
                        {
                            originalFileStream.CopyTo(compressionStream);
                        }
                    }
                }
            }
        }


    }
}
