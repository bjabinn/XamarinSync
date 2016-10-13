using Android.App;
using Android.OS;
using Android.Widget;

namespace Shared.Droid
{
	[Activity (Label = "Shared.Droid", MainLauncher = true, Icon = "@drawable/icon")]
	public class MainActivity : Activity
	{
		protected override void OnCreate (Bundle bundle)
		{
			base.OnCreate (bundle);

			SetContentView (Resource.Layout.Main);

			var button = FindViewById<Button> (Resource.Id.myButton);
			
			button.Click += delegate {
                var a = new ServiceManagement();
                a.DoWork();				
			};
		}
	}
}


