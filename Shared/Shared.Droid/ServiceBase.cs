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
    public class ServiceBase : Service
    {
        DemoServiceBinder _binder;

        public override IBinder OnBind(Intent intent)
        {
            _binder = new DemoServiceBinder(this);
            return _binder;
        }

        public override void OnDestroy()
        {
            base.OnDestroy();

            Log.Debug("ServiceBase", "ServiceBase stopped");
        }

        public virtual bool DoWork()
        {
            return false;
        }

        public override StartCommandResult OnStartCommand(Android.Content.Intent intent, StartCommandFlags flags, int startId)
        {
            return StartCommandResult.NotSticky;
        }

        public class DemoServiceBinder : Binder
        {
            ServiceBase _service;

            public DemoServiceBinder(ServiceBase service)
            {
                this._service = service;
            }

            public ServiceBase GetDemoService()
            {
                return _service;
            }
        }

        

    }
}

