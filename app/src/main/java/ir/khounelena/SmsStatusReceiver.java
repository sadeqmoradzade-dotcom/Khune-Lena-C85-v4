package ir.khounelena;
import android.app.Activity; import android.content.*; import android.widget.Toast;
public class SmsStatusReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){ int r=getResultCode(); String t=i.getStringExtra("title"); Toast.makeText(c,r==Activity.RESULT_OK?"SMS با موفقیت ارسال شد: "+t:"ارسال SMS ناموفق — کد خطا: "+r,Toast.LENGTH_LONG).show(); }
}
