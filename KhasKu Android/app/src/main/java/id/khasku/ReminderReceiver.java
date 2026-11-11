package id.khasku;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL="daily_reminder";
    static void schedule(Context context){
        Calendar next=Calendar.getInstance();next.set(Calendar.HOUR_OF_DAY,20);next.set(Calendar.MINUTE,0);next.set(Calendar.SECOND,0);if(next.getTimeInMillis()<=System.currentTimeMillis())next.add(Calendar.DAY_OF_YEAR,1);
        AlarmManager alarm=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),AlarmManager.INTERVAL_DAY,pending(context));
    }
    private static PendingIntent pending(Context c){return PendingIntent.getBroadcast(c,1,new Intent(c,ReminderReceiver.class).setAction("id.khasku.DAILY_REMINDER"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    @Override public void onReceive(Context context,Intent intent){
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){if(context.getSharedPreferences("khasku_local",Context.MODE_PRIVATE).getBoolean("reminder",true))schedule(context);return;}
        NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(new NotificationChannel(CHANNEL,"Pengingat harian",NotificationManager.IMPORTANCE_DEFAULT));
        PendingIntent open=PendingIntent.getActivity(context,2,new Intent(context,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification notification=new android.app.Notification.Builder(context,CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("KhasKu").setContentText("Jangan lupa catat pengeluaranmu hari ini.").setContentIntent(open).setAutoCancel(true).build();
        try{manager.notify(1,notification);}catch(SecurityException ignored){}
    }
}
