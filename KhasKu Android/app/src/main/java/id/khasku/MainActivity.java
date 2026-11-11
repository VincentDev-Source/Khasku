package id.khasku;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.Bundle;
import android.os.Build;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.Intent;
import android.util.TypedValue;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int TEAL=0xff147c72, DARK=0xff173a38, BG=0xfff6f8fb, MUTED=0xff718087;
    private static final String[] GROUPS={"Kebutuhan","Keinginan","Tabungan"};
    private static final String[] CATEGORIES={"Makan","Transportasi","Kuliah","Kos","Pulsa & Data","Jalan-jalan","Belanja"};
    private static final int[] COLORS={0xff15977f,0xffec8650,0xff665de8};
    private SharedPreferences prefs;
    private LinearLayout page, content;
    private String screen="Beranda", selectedGroup="Kebutuhan", selectedCategory="Makan";
    private EditText amountInput, noteInput;
    private int period=0;
    private String draftAmount="", draftNote="";
    private String historyFilter="Semua", historyCategory="";

    @Override public void onCreate(Bundle state){super.onCreate(state); getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(Color.WHITE); getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR); prefs=getSharedPreferences("khasku_local",MODE_PRIVATE); render();}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private String money(long n){return "Rp"+NumberFormat.getInstance(new Locale("id","ID")).format(n);}
    private long budget(){return prefs.getLong("budget",0);}
    private long spent(){return spentSince(periodStart());}
    private long spentSince(long from){long sum=0;JSONArray a=transactions();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&o.optLong("date")>=from)sum+=o.optLong("amount");}return sum;}
    private long periodStart(){Calendar c=Calendar.getInstance();int today=c.get(Calendar.DAY_OF_MONTH),day=prefs.getInt("payoutDay",1);c.set(Calendar.DAY_OF_MONTH,1);int effectiveDay=Math.min(day,c.getActualMaximum(Calendar.DAY_OF_MONTH));if(today<effectiveDay)c.add(Calendar.MONTH,-1);c.set(Calendar.DAY_OF_MONTH,Math.min(day,c.getActualMaximum(Calendar.DAY_OF_MONTH)));c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c.getTimeInMillis();}
    private long weekStart(){Calendar c=Calendar.getInstance();c.set(Calendar.DAY_OF_WEEK,c.getFirstDayOfWeek());c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c.getTimeInMillis();}
    private int share(int index){return prefs.getInt("share"+index,new int[]{50,30,20}[index]);}
    private JSONArray transactions(){try{return new JSONArray(prefs.getString("transactions","[]"));}catch(Exception e){return new JSONArray();}}
    private void render(){
        page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setBackgroundColor(BG); setContentView(page);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(20),dp(12),dp(20),dp(24)); scroll.addView(content);
        if(budget()==0) onboarding(); else if(!prefs.getBoolean("setupDone",false)) onboarding2(); else switch(screen){case "Catat":history();break;case "Rekap":summary();break;case "Tantangan":challenge();break;case "Transaksi":quickEntry();break;case "Proyeksi":forecast();break;case "Anggaran":budgetSetup(false);break;default:home();}
        if(budget()>0 && prefs.getBoolean("setupDone",false) && !screen.equals("Transaksi")&&!screen.equals("Proyeksi")&&!screen.equals("Anggaran")) bottomNav();
    }
    private void onboarding(){
        text(content,"LANGKAH 1 DARI 2",12,MUTED,true); text(content,"Atur anggaran bulananmu",23,DARK,true); text(content,"Mulai dengan jumlah yang ingin kamu kelola bulan ini.",14,MUTED,false);
        EditText amount=edit("1500000",true); content.addView(amount,space(52));
        text(content,"Aturan 50/30/20",15,DARK,true); text(content,"Kebutuhan, keinginan, dan tabungan",13,MUTED,false);
        for(int i=0;i<3;i++)rowCard(GROUPS[i],new String[]{"Makan, transportasi, kos","Jajan, hiburan, gaya hidup","Tabungan masa depan"}[i],new int[]{50,30,20}[i]+"%",COLORS[i]);
        button(content,"Pakai aturan ini",()->{long n=parseAmount(amount.getText().toString()); if(n>0){prefs.edit().putLong("budget",n).putInt("share0",50).putInt("share1",30).putInt("share2",20).apply(); screen="Beranda";render();}else toast("Masukkan jumlah anggaran yang valid");});
        button(content,"Atur sendiri",()->{long n=parseAmount(amount.getText().toString());if(n>0){prefs.edit().putLong("budget",n).apply(); screen="Anggaran";render();}else toast("Masukkan jumlah anggaran yang valid");});
    }
    private void onboarding2(){text(content,"LANGKAH 2 DARI 2",12,MUTED,true);text(content,"Kapan uang sakumu masuk?",23,DARK,true);text(content,"Pilih tanggal yang paling sesuai untuk mengatur periode anggaranmu.",14,MUTED,false);int day=prefs.getInt("payoutDay",1);LinearLayout dates=row(content);for(int d:new int[]{1,5,15,25,30})pill(dates,"Tgl "+d,()->{prefs.edit().putInt("payoutDay",d).apply();render();},day==d);rowCard("Periode kamu: Tgl "+day+"–"+(day==1?"30":day-1),"Reset otomatis setiap periode", "◷",TEAL);cardTitle("Reminder Dompet Aman");text(content,"Catat pengeluaranmu setiap malam.",13,MUTED,false);Switch reminder=new Switch(this);reminder.setText("Ingatkan aku pukul 20.00");reminder.setTextColor(DARK);reminder.setChecked(prefs.getBoolean("reminder",true));content.addView(reminder,space(52));reminder.setOnCheckedChangeListener((v,checked)->{prefs.edit().putBoolean("reminder",checked).apply();if(checked)scheduleReminder();else cancelReminder();});button(content,"Mulai pakai KhasKu →",()->{prefs.edit().putBoolean("setupDone",true).apply();if(prefs.getBoolean("reminder",true))scheduleReminder();screen="Beranda";render();});}
    private void home(){
        header(); text(content,"ACUAN BELANJA HARI INI",11,MUTED,true); long remaining=Math.max(0,budget()-spent()); text(content,money(remaining),31,TEAL,true); text(content,money(budget())+" / bulan · total terpakai "+money(spent()),13,MUTED,false);
        cardTitle("Anggaran "+monthly()); for(int i=0;i<3;i++){long part=budget()*share(i)/100; long used=groupSpent(GROUPS[i]); budgetRow(GROUPS[i],used,part,COLORS[i]);}
        LinearLayout actions=row(content); pill(actions,"+ Catat Cepat",()->{screen="Transaksi";render();},true); pill(actions,"Proyeksi",()->{screen="Proyeksi";render();},false);
        cardTitle("Transaksi terbaru"); addTransactionRows(3);
    }
    private void header(){LinearLayout r=row(content); text(r,"◉  KhasKu",18,DARK,true); View spacer=new View(this);r.addView(spacer,new LinearLayout.LayoutParams(0,1,1)); text(r,"●",18,TEAL,true);}
    private void budgetRow(String label,long used,long limit,int color){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(12),dp(9),dp(12),dp(9));content.addView(box,space(58));LinearLayout r=row(box);text(r,"●  "+label,14,DARK,true);View v=new View(this);r.addView(v,new LinearLayout.LayoutParams(0,1,1));text(r,money(used)+" / "+money(limit),12,MUTED,false);FrameLayout bar=new FrameLayout(this);bar.setBackground(shape(0xffe5ebf1,99));box.addView(bar,space(6));View fill=new View(this);fill.setBackground(shape(color,99));bar.addView(fill,new FrameLayout.LayoutParams(0,-1,Gravity.START));bar.post(()->{int width=(int)(bar.getWidth()*Math.min(1,limit==0?0:(double)used/limit));FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)fill.getLayoutParams();lp.width=width;fill.setLayoutParams(lp);});}
    private void history(){header();text(content,"Riwayat Transaksi",23,DARK,true);text(content,"Semua catatan pengeluaranmu",13,MUTED,false);LinearLayout filters=row(content);pill(filters,"Semua",()->{historyFilter="Semua";render();},historyFilter.equals("Semua"));pill(filters,"Hari ini",()->{historyFilter="Hari ini";render();},historyFilter.equals("Hari ini"));pill(filters,historyCategory.isEmpty()?"Kategori":historyCategory,()->new AlertDialog.Builder(this).setTitle("Pilih kategori").setItems(CATEGORIES,(dialog,which)->{historyCategory=CATEGORIES[which];historyFilter="Kategori";render();}).show(),historyFilter.equals("Kategori"));addTransactionRows(0);LinearLayout r=row(content);pill(r,"+ Catat Cepat",()->{screen="Transaksi";render();},true);}
    private void summary(){header();text(content,"Ringkasan",23,DARK,true);LinearLayout r=row(content);pill(r,"Mingguan",()->{period=0;render();},period==0);pill(r,"Bulanan",()->{period=1;render();},period==1);long start=period==0?weekStart():periodStart();long amount=spentSince(start);cardTitle(period==0?"Pengeluaran minggu ini":"Pengeluaran bulan ini");text(content,money(amount),27,TEAL,true);text(content,"Anggaran "+money(budget()),14,MUTED,false);for(int i=0;i<3;i++)budgetRow(GROUPS[i],groupSpentSince(GROUPS[i],start),budget()*share(i)/100,COLORS[i]);cardTitle("Pengeluaran terbesar");addTopCategoryRows(start);}
    private void challenge(){header();text(content,"Jejak Kebiasaan",23,DARK,true);text(content,"Tantangan minggu ini",14,MUTED,false);cardTitle("Jaga pengeluaran tetap teratur");text(content,"Catat pengeluaranmu setiap hari dan lihat kebiasaan belanja yang terbentuk.",15,DARK,false);cardTitle("Progres minggu ini");text(content,activeDaysThisWeek()+" dari 7 hari",20,TEAL,true);text(content,"Konsisten mencatat membantu kamu tetap sesuai anggaran.",13,MUTED,false);button(content,"+ Catat Cepat",()->{screen="Transaksi";render();});}
    private void forecast(){header();text(content,"‹  Acuan Belanja Harian",20,DARK,true);long daily=Math.max(0,(budget()-spent())/daysToNextPayout());text(content,money(daily)+" / hari",28,TEAL,true);text(content,"Proyeksi sisa belanja",16,DARK,true);cardTitle("Rencana perhitungan");text(content,"Sisa belanja    "+money(Math.max(0,budget()-spent())),15,DARK,true);text(content,"Sisa waktu      "+daysToNextPayout()+" hari",15,DARK,false);button(content,"+ Catat Lagi",()->{screen="Transaksi";render();});button(content,"Kembali",()->{screen="Beranda";render();});}
    private void budgetSetup(boolean unused){text(content,"‹  Atur Anggaran",22,DARK,true);EditText amount=edit(String.valueOf(budget()),true);content.addView(amount,space(52));text(content,"Pembagian anggaran (%)",16,DARK,true);EditText[] shares=new EditText[3];for(int i=0;i<3;i++){rowCard(GROUPS[i],"Bagian dari anggaran bulanan",share(i)+"%",COLORS[i]);shares[i]=edit(String.valueOf(share(i)),true);content.addView(shares[i],space(48));}button(content,"Simpan Anggaran",()->{long n=parseAmount(amount.getText().toString());int[] values=new int[3];int total=0;for(int i=0;i<3;i++){long p=parseAmount(shares[i].getText().toString());if(p>100){toast("Persentase harus 0–100");return;}values[i]=(int)p;total+=values[i];}if(n<=0){toast("Masukkan jumlah yang valid");return;}if(total!=100){toast("Total pembagian harus 100%");return;}SharedPreferences.Editor e=prefs.edit().putLong("budget",n);for(int i=0;i<3;i++)e.putInt("share"+i,values[i]);e.apply();screen="Beranda";render();});}
    private void quickEntry(){
        TextView back=text(content,"‹  Catat Cepat",21,DARK,true);back.setOnClickListener(v->{drafts();screen="Beranda";render();});text(content,"PENGELUARAN",11,TEAL,true);amountInput=edit(draftAmount,true);amountInput.setHint("Rp 0");amountInput.setTextSize(TypedValue.COMPLEX_UNIT_SP,30);content.addView(amountInput,space(64));text(content,"Kategori transaksi",15,DARK,true);
        LinearLayout g=row(content); for(String s:GROUPS)pill(g,s,()->{drafts();selectedGroup=s;render();},selectedGroup.equals(s));LinearLayout c1=row(content);for(int i=0;i<4;i++){final String c=CATEGORIES[i];pill(c1,c,()->{drafts();selectedCategory=c;render();},selectedCategory.equals(c));} LinearLayout c2=row(content);for(int i=4;i<CATEGORIES.length;i++){final String c=CATEGORIES[i];pill(c2,c,()->{drafts();selectedCategory=c;render();},selectedCategory.equals(c));}
        text(content,"Detail (opsional)",14,DARK,true);noteInput=edit(draftNote,false);noteInput.setHint("Catatan transaksi");content.addView(noteInput,space(50));button(content,"Simpan Transaksi  ✓",this::saveTransaction);text(content,"Tersimpan hanya di perangkat ini",12,MUTED,false);
    }
    private void saveTransaction(){long n=parseAmount(amountInput.getText().toString());if(n<=0){toast("Masukkan nominal yang valid");return;}JSONArray a=transactions();JSONObject o=new JSONObject();try{o.put("amount",n);o.put("category",selectedCategory);o.put("group",selectedGroup);o.put("note",noteInput.getText().toString().trim());o.put("date",System.currentTimeMillis());a.put(o);}catch(Exception ignored){}prefs.edit().putString("transactions",a.toString()).apply();draftAmount="";draftNote="";screen="Beranda";render();toast("Transaksi tersimpan");}
    private void addTransactionRows(int limit){JSONArray a=transactions(); if(a.length()==0){text(content,"Belum ada transaksi",14,MUTED,false);return;}int shown=0;String today=new SimpleDateFormat("yyyyMMdd",Locale.ROOT).format(new Date());for(int i=a.length()-1;i>=0&&(limit==0||shown<limit);i--){JSONObject o=a.optJSONObject(i);if(o==null)continue;String cat=o.optString("category","Lainnya");if(screen.equals("Catat")&&historyFilter.equals("Hari ini")&&!today.equals(new SimpleDateFormat("yyyyMMdd",Locale.ROOT).format(new Date(o.optLong("date")))))continue;if(screen.equals("Catat")&&historyFilter.equals("Kategori")&&!historyCategory.equals(cat))continue;String note=o.optString("note","");LinearLayout r=row(content);TextView icon=text(r,"●",18,TEAL,true);icon.setPadding(dp(4),dp(4),dp(12),dp(4));LinearLayout name=new LinearLayout(this);name.setOrientation(1);r.addView(name,new LinearLayout.LayoutParams(0,-2,1));text(name,note.isEmpty()?cat:note,14,DARK,true);text(name,cat+" · "+new SimpleDateFormat("d MMM",new Locale("id")).format(new Date(o.optLong("date"))),11,MUTED,false);text(r,"−"+money(o.optLong("amount")),14,DARK,true);r.setPadding(0,dp(10),0,dp(10));shown++;}if(shown==0)text(content,"Tidak ada transaksi untuk filter ini",14,MUTED,false);}
    private void addTopCategoryRows(long from){long[] totals=new long[CATEGORIES.length];int[] order=new int[CATEGORIES.length];for(int i=0;i<order.length;i++)order[i]=i;JSONArray a=transactions();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null||o.optLong("date")<from)continue;for(int j=0;j<CATEGORIES.length;j++)if(CATEGORIES[j].equals(o.optString("category")))totals[j]+=o.optLong("amount");}for(int i=0;i<order.length;i++)for(int j=i+1;j<order.length;j++)if(totals[order[j]]>totals[order[i]]){int n=order[i];order[i]=order[j];order[j]=n;}int count=0;for(int idx:order){if(count>=3||totals[idx]==0)break;rowCard(CATEGORIES[idx],"Pengeluaran",money(totals[idx]),COLORS[count%COLORS.length]);count++;}if(count==0)text(content,"Belum ada transaksi di periode ini.",14,MUTED,false);}
    private long groupSpent(String group){return groupSpentSince(group,periodStart());}
    private long groupSpentSince(String group,long from){long sum=0;JSONArray a=transactions();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&group.equals(o.optString("group"))&&o.optLong("date")>=from)sum+=o.optLong("amount");}return sum;}
    private int activeDaysThisWeek(){java.util.HashSet<String> days=new java.util.HashSet<>();JSONArray a=transactions();SimpleDateFormat f=new SimpleDateFormat("yyyyMMdd",Locale.ROOT);long start=weekStart();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&o.optLong("date")>=start)days.add(f.format(new Date(o.optLong("date"))));}return days.size();}
    private int daysToNextPayout(){Calendar today=Calendar.getInstance();today.set(Calendar.HOUR_OF_DAY,0);today.set(Calendar.MINUTE,0);today.set(Calendar.SECOND,0);today.set(Calendar.MILLISECOND,0);int payday=prefs.getInt("payoutDay",1);Calendar next=(Calendar)today.clone();next.set(Calendar.DAY_OF_MONTH,1);int effectiveDay=Math.min(payday,next.getActualMaximum(Calendar.DAY_OF_MONTH));if(today.get(Calendar.DAY_OF_MONTH)>=effectiveDay)next.add(Calendar.MONTH,1);next.set(Calendar.DAY_OF_MONTH,Math.min(payday,next.getActualMaximum(Calendar.DAY_OF_MONTH)));return Math.max(1,(int)((next.getTimeInMillis()-today.getTimeInMillis())/86400000L));}
    private void bottomNav(){LinearLayout nav=new LinearLayout(this);nav.setBackgroundColor(Color.WHITE);nav.setElevation(dp(8));page.addView(nav,new LinearLayout.LayoutParams(-1,dp(62)));String[] labels={"Beranda","Catat","Rekap","Tantangan"};for(String s:labels){TextView t=text(nav,s.equals("Beranda")?"⌂\n"+s:s.equals("Catat")?"≡\n"+s:s.equals("Rekap")?"◷\n"+s:"◇\n"+s,11,screen.equals(s)?TEAL:MUTED,screen.equals(s));t.setGravity(Gravity.CENTER);nav.addView(t,new LinearLayout.LayoutParams(0,-1,1));t.setOnClickListener(v->{screen=s;render();});}}
    private void cardTitle(String s){TextView t=text(content,s,16,DARK,true);t.setPadding(0,dp(13),0,dp(7));}
    private void rowCard(String title,String sub,String value,int color){LinearLayout r=row(content);r.setPadding(dp(12),dp(12),dp(12),dp(12));r.setBackground(shape(Color.WHITE,dp(12)));TextView dot=text(r,"●",14,color,true);dot.setPadding(0,0,dp(9),0);LinearLayout copy=new LinearLayout(this);copy.setOrientation(1);r.addView(copy,new LinearLayout.LayoutParams(0,-2,1));text(copy,title,14,DARK,true);text(copy,sub,11,MUTED,false);text(r,value,13,DARK,true);LinearLayout.LayoutParams lp=space(55);lp.bottomMargin=dp(7);content.addView(r,lp);}
    private void button(LinearLayout parent,String label,Runnable action){TextView b=text(parent,label,14,Color.WHITE,true);b.setGravity(Gravity.CENTER);b.setBackground(shape(TEAL,dp(12)));b.setPadding(dp(12),dp(14),dp(12),dp(14));LinearLayout.LayoutParams lp=space(52);lp.topMargin=dp(8);parent.addView(b,lp);b.setOnClickListener(v->action.run());}
    private void pill(LinearLayout parent,String label,Runnable action,boolean active){TextView t=text(parent,label,12,active?Color.WHITE:TEAL,true);t.setGravity(Gravity.CENTER);t.setBackground(shape(active?TEAL:0xffe7f3f1,dp(24)));t.setPadding(dp(12),dp(9),dp(12),dp(9));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.setMargins(0,dp(5),dp(7),dp(5));parent.addView(t,lp);t.setOnClickListener(v->action.run());}
    private EditText edit(String val,boolean numeric){EditText e=new EditText(this);e.setSingleLine(true);e.setText(val);e.setTextColor(DARK);e.setTextSize(20);e.setPadding(dp(12),0,dp(12),0);e.setBackground(shape(Color.WHITE,dp(12)));if(numeric)e.setInputType(InputType.TYPE_CLASS_NUMBER);return e;}
    private TextView text(LinearLayout parent,String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.bottomMargin=dp(5);parent.addView(t,lp);return t;}
    private LinearLayout row(LinearLayout parent){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.setGravity(Gravity.CENTER_VERTICAL);parent.addView(r,space(-2));return r;}
    private LinearLayout.LayoutParams space(int h){return new LinearLayout.LayoutParams(-1,h<0?h:dp(h));}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);return d;}
    private long parseAmount(String s){try{return Long.parseLong(s.replaceAll("[^0-9]",""));}catch(Exception e){return 0;}}
    private void drafts(){if(amountInput!=null)draftAmount=amountInput.getText().toString();if(noteInput!=null)draftNote=noteInput.getText().toString();}
    private String monthly(){return new SimpleDateFormat("MMMM",Locale.forLanguageTag("id-ID")).format(new Date(periodStart()));}
    private void scheduleReminder(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},20);return;}ReminderReceiver.schedule(this);}
    private void cancelReminder(){AlarmManager alarm=(AlarmManager)getSystemService(ALARM_SERVICE);alarm.cancel(PendingIntent.getBroadcast(this,1,new Intent(this,ReminderReceiver.class).setAction("id.khasku.DAILY_REMINDER"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));}
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){super.onRequestPermissionsResult(requestCode,permissions,results);if(requestCode==20&&results.length>0&&results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)ReminderReceiver.schedule(this);else if(requestCode==20){prefs.edit().putBoolean("reminder",false).apply();toast("Izin notifikasi tidak diberikan");render();}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
