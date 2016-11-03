using System.Threading;
using Android.App;
using Android.Util;


namespace Shared.Droid
{
    [Service]
    public class ServiceManagement : ServiceBase
    {
       public override bool DoWork()
        {            
            var maxSync = 800;
            
            Log.Debug("ServiceManagement", "Service started");

            var t = new Thread(() =>
            {
                var uniqueInstaceToSync = new SyncService();
                var xmlRequest = uniqueInstaceToSync.ConstructXml(null, false, false, maxSync);

                if (string.IsNullOrEmpty(xmlRequest))
                {
                    return;
                }
                
                var bytes = uniqueInstaceToSync.PostSync(xmlRequest);
                if (bytes == null)
                {
                    if (uniqueInstaceToSync.LastTransmissionFailed)
                    {
                        //SyncFailedException();
                    }
                    return;
                }

                var doc = new XmlHelper();
                var xmlIncoming = doc.ByteArrayToXml(bytes);
                if (xmlIncoming == null)
                {
                    //syncFailed();
                    //return false;
                }
                                
                Thread.Sleep(50000);

                Log.Debug("ServiceManagement", "Stopping foreground");
                StopForeground(true);

                StopSelf();
            }
            );

            t.Start();
           return true;
        } //end DoWork



    } //end class
} //end namespace

