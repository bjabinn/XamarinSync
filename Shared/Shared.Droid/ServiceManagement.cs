using System.Threading;
using Android.App;
using Android.Content;
using Android.OS;
using Android.Util;
using Android.Widget;
using Java.IO;

namespace Shared.Droid
{
    [Service]
    public class ServiceManagement : ServiceBase
    {
       public override bool DoWork()
        {
            var forced = false;
            var maxSync = 800;
            
            Log.Debug("ServiceManagement", "Service started");

            var t = new Thread(() =>
            {
                var uniqueInstaceToSync = new SyncService();
                var xmlRequest = uniqueInstaceToSync.ConstructXml(null, forced, false, maxSync);

                if (string.IsNullOrEmpty(xmlRequest))
                {
                    return true;
                }

                byte[] bytes = uniqueInstaceToSync.PostSync(xmlRequest);
                if (bytes == null)
                {
                    if (uniqueInstaceToSync.LastTransmissionFailed)
                    {
                        SyncFailedException();
                    }
                    return false;
                }

                Document doc = getDocFromBytes(bytes);
                if (doc == null)
                {
                    syncFailed();
                    return false;
                }
                                
                Thread.Sleep(12000);

                Log.Debug("ServiceManagement", "Stopping foreground");
                StopForeground(true);

                StopSelf();
            }
            );

            t.Start();
        }



    }
}

