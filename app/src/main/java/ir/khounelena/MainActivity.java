package ir.khounelena;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.telephony.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private static final int REQ=1001;
    private EditText phoneNumber; private Spinner simSpinner; private TextView statusText;
    private final ArrayList<Integer> subs=new ArrayList<>();
    private static final String[] ON={"*000000*21*1#","*000000*21*2#","*000000*21*3#","*000000*21*4#","*000000*21*5#","*000000*21*6#","*000000*21*7#"};
    private static final String[] OFF={"*000000*20*1#","*000000*20*2#","*000000*20*3#","*000000*20*4#","*000000*20*5#","*000000*20*6#","*000000*20*7#"};

    @Override protected void onCreate(Bundle b){
        super.onCreate(b); setContentView(R.layout.activity_main);
        phoneNumber=findViewById(R.id.phoneNumber); simSpinner=findViewById(R.id.simSpinner); statusText=findViewById(R.id.statusText);
        setupSims(); setupLights();
        findViewById(R.id.alarmOn).setOnClickListener(v->sendSms("*000000*11#","دزدگیر فعال"));
        findViewById(R.id.alarmOff).setOnClickListener(v->sendSms("*000000*10#","دزدگیر غیرفعال"));
        findViewById(R.id.testButton).setOnClickListener(v->sendSms("*000000*11#","تست"));
        findViewById(R.id.travelButton).setOnClickListener(v->travel());
    }
    private void setupSims(){
        ArrayList<String> labels=new ArrayList<>(); labels.add("انتخاب خودکار / پیش‌فرض"); subs.add(-1);
        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.READ_PHONE_STATE)==PackageManager.PERMISSION_GRANTED){
            try{
                List<SubscriptionInfo> list=getSystemService(SubscriptionManager.class).getActiveSubscriptionInfoList();
                if(list!=null) for(SubscriptionInfo x:list){ subs.add(x.getSubscriptionId()); labels.add((x.getCarrierName()==null?"SIM":x.getCarrierName().toString())+" — SIM "+(x.getSimSlotIndex()+1)); }
            }catch(Exception ignored){}
        }
        simSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));
    }
    private void setupLights(){
        LinearLayout box=findViewById(R.id.lightsContainer);
        for(int i=0;i<7;i++){ final int n=i;
            Button a=new Button(this); a.setText("روشنایی "+(i+1)+" — روشن"); a.setOnClickListener(v->sendSms(ON[n],"روشنایی "+(n+1)+" روشن")); box.addView(a);
            Button d=new Button(this); d.setText("روشنایی "+(i+1)+" — خاموش"); d.setOnClickListener(v->sendSms(OFF[n],"روشنایی "+(n+1)+" خاموش")); box.addView(d);
        }
    }
    private int chosenSub(){ int p=simSpinner.getSelectedItemPosition(); return p>=0&&p<subs.size()?subs.get(p):-1; }

    private SmsManager smsManager(){
        int sub=chosenSub();
        if(sub!=-1) return Build.VERSION.SDK_INT>=31 ? getSystemService(SmsManager.class).createForSubscriptionId(sub) : SmsManager.getSmsManagerForSubscriptionId(sub);
        int def=SmsManager.getDefaultSmsSubscriptionId();
        if(def!=-1) return Build.VERSION.SDK_INT>=31 ? getSystemService(SmsManager.class).createForSubscriptionId(def) : SmsManager.getSmsManagerForSubscriptionId(def);
        return null;
    }

    public void sendSms(String message,String title){
        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.SEND_SMS)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.SEND_SMS,Manifest.permission.READ_PHONE_STATE},REQ); toast("اجازه SMS را بدهید و دوباره امتحان کنید"); return;
        }
        String number=phoneNumber.getText().toString().trim();
        if(number.isEmpty()){toast("شماره دستگاه را وارد کنید");return;}
        try{
            SmsManager sms=smsManager();
            if(sms==null){statusText.setText("سیم‌کارت ارسال SMS انتخاب نشده است؛ SIM 1 یا SIM 2 را انتخاب کنید.");return;}
            Intent sent=new Intent(this,SmsStatusReceiver.class); sent.putExtra("title",title);
            PendingIntent pi=PendingIntent.getBroadcast(this,(int)(System.currentTimeMillis()&0x7fffffff),sent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            sms.sendTextMessage(number,null,message,pi,null);
            statusText.setText("در حال ارسال با SIM انتخاب‌شده: "+message);
        }catch(Exception e){statusText.setText("خطای ارسال: "+e.getClass().getSimpleName()+" — "+e.getMessage());}
    }

    private void travel(){
        if(phoneNumber.getText().toString().trim().isEmpty()){toast("شماره دستگاه را وارد کنید");return;}
        sendSms("*000000*11#","حالت سفر: دزدگیر فعال");
        schedule(18,ON[0],"روشنایی ۱ ساعت ۱۸"); schedule(0,OFF[0],"روشنایی ۱ ساعت ۲۴");
        statusText.setText("حالت سفر فعال شد.");
    }
    private void schedule(int hour,String message,String title){
        Intent i=new Intent(this,ScheduleReceiver.class); i.putExtra("number",phoneNumber.getText().toString().trim()); i.putExtra("subscriptionId",chosenSub()); i.putExtra("message",message); i.putExtra("title",title);
        int req=hour==18?1801:2401; PendingIntent pi=PendingIntent.getBroadcast(this,req,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Calendar c=Calendar.getInstance(); c.set(Calendar.HOUR_OF_DAY,hour); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0);
        if(c.getTimeInMillis()<=System.currentTimeMillis())c.add(Calendar.DAY_OF_YEAR,1);
        AlarmManager am=getSystemService(AlarmManager.class); if(Build.VERSION.SDK_INT>=31&&am.canScheduleExactAlarms())am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi); else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi);
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
