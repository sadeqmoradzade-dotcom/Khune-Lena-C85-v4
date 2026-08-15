package ir.khounelena;
import android.app.*; import android.content.*; import android.os.Build; import android.telephony.*; import java.util.*;
public class ScheduleReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){
  String number=i.getStringExtra("number"), message=i.getStringExtra("message"), title=i.getStringExtra("title"); int sub=i.getIntExtra("subscriptionId",-1);
  if(number==null||number.trim().isEmpty()||message==null)return;
  try{
   SmsManager sms;
   if(sub!=-1) sms=Build.VERSION.SDK_INT>=31?c.getSystemService(SmsManager.class).createForSubscriptionId(sub):SmsManager.getSmsManagerForSubscriptionId(sub);
   else { int d=SmsManager.getDefaultSmsSubscriptionId(); if(d==-1)return; sms=Build.VERSION.SDK_INT>=31?c.getSystemService(SmsManager.class).createForSubscriptionId(d):SmsManager.getSmsManagerForSubscriptionId(d); }
   Intent sent=new Intent(c,SmsStatusReceiver.class); sent.putExtra("title",title);
   PendingIntent pi=PendingIntent.getBroadcast(c,(int)(System.currentTimeMillis()&0x7fffffff),sent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   sms.sendTextMessage(number.trim(),null,message,pi,null);
  }catch(Exception ignored){}
  int hour=message.contains("*21*1#")?18:0; Calendar cal=Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR,1); cal.set(Calendar.HOUR_OF_DAY,hour); cal.set(Calendar.MINUTE,0); cal.set(Calendar.SECOND,0); cal.set(Calendar.MILLISECOND,0);
  Intent n=new Intent(c,ScheduleReceiver.class); n.putExtra("number",number); n.putExtra("subscriptionId",sub); n.putExtra("message",message); n.putExtra("title",title);
  PendingIntent np=PendingIntent.getBroadcast(c,hour==18?1801:2401,n,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE); if(Build.VERSION.SDK_INT>=31&&am.canScheduleExactAlarms())am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),np); else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),np);
 }
}
