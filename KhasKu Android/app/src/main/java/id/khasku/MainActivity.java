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
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.Spinner;
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
    private static final int TEAL=0xff147c72, DARK=0xff172033, BG=0xfff6f8fb, MUTED=0xff718087;
    private static final String[] GROUPS={"Kebutuhan","Keinginan","Tabungan"};
    private static final String[] CATEGORIES={"Makan","Transportasi","Kuliah","Kos","Pulsa & Data","Jalan-jalan","Belanja","Top-up gim","Kopi susu aren","Parkir kampus","Langganan musik"};
    private static final int[] COLORS={0xff15977f,0xffec8650,0xff665de8};
    private SharedPreferences prefs;
    private LinearLayout page, content;
    private String screen="Beranda", selectedGroup="Kebutuhan", selectedCategory="Makan";
    private EditText amountInput, noteInput;
    private int period=0;
    private String draftAmount="", draftNote="";
    private String historyFilter="Semua", historyCategory="";
    private boolean selectedCrumbs;
    private boolean lastSavedCrumbs;
    private long lastSavedAmount;

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
        if(budget()==0) onboarding(); else if(screen.equals("Anggaran")) budgetSetup(false); else if(!prefs.getBoolean("setupDone",false)) onboarding2(); else switch(screen){case "Catat":history();break;case "Recehan":recehan();break;case "Ringkasan":summary();break;case "Transaksi":quickEntry();break;case "Tersimpan":saved();break;case "Proyeksi":forecast();break;default:home();}
        if(budget()>0 && prefs.getBoolean("setupDone",false) && !screen.equals("Transaksi")&&!screen.equals("Tersimpan")&&!screen.equals("Proyeksi")&&!screen.equals("Anggaran")) bottomNav();
    }
    private void onboarding(){
        progressIndicator(1);
        text(content,"LANGKAH 1 DARI 2",12,MUTED,true); text(content,"Atur anggaran bulananmu",23,DARK,true); text(content,"Mulai dengan jumlah yang ingin kamu kelola bulan ini.",14,MUTED,false);
        EditText amount=edit("1500000",true); content.addView(amount,space(52));
        text(content,"Aturan 50/30/20",15,DARK,true); text(content,"Kebutuhan, keinginan, dan tabungan",13,MUTED,false);
        for(int i=0;i<3;i++)rowCard(GROUPS[i],new String[]{"Makan, transportasi, kos","Jajan, hiburan, gaya hidup","Tabungan masa depan"}[i],new int[]{50,30,20}[i]+"%",COLORS[i]);
        button(content,"Pakai aturan ini",()->{long n=parseAmount(amount.getText().toString()); if(n>0){prefs.edit().putLong("budget",n).putInt("share0",50).putInt("share1",30).putInt("share2",20).apply(); screen="Beranda";render();}else toast("Masukkan jumlah anggaran yang valid");});
        button(content,"Atur sendiri",()->{long n=parseAmount(amount.getText().toString());if(n>0){prefs.edit().putLong("budget",n).apply(); screen="Anggaran";render();}else toast("Masukkan jumlah anggaran yang valid");});
    }
    private void onboarding2(){
        LinearLayout step=row(content);
        TextView back=text(step,"‹",28,DARK,true);
        back.setOnClickListener(v->{screen="Anggaran";render();});
        View spacer=new View(this);
        step.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
        text(step,"LANGKAH 2 DARI 2",12,MUTED,true);
        View trailingSpacer=new View(this);
        step.addView(trailingSpacer,new LinearLayout.LayoutParams(0,1,1));
        progressIndicator(2);
        text(content,"Kapan uang sakumu masuk?",23,DARK,true);
        text(content,"Pilih tanggal yang paling sesuai untuk mengatur periode anggaranmu.",14,MUTED,false);
        int day=prefs.getInt("payoutDay",1);
        LinearLayout dates=row(content);
        for(int d:new int[]{1,5,15,25,30})pill(dates,"Tgl "+d,()->{prefs.edit().putInt("payoutDay",d).apply();render();},day==d);
        rowCard("Periode kamu: Tgl "+day+"–"+(day==1?"30":day-1),"Reset otomatis setiap periode","◷",TEAL);
        cardTitle("Reminder Dompet Aman");
        text(content,"Catat pengeluaranmu setiap malam.",13,MUTED,false);
        Switch reminder=new Switch(this);
        reminder.setText("Ingatkan aku pukul 20.00");
        reminder.setTextColor(DARK);
        reminder.setChecked(prefs.getBoolean("reminder",true));
        content.addView(reminder,space(52));
        reminder.setOnCheckedChangeListener((v,checked)->{prefs.edit().putBoolean("reminder",checked).apply();if(checked)scheduleReminder();else cancelReminder();});
        button(content,"Mulai pakai KhasKu →",()->{prefs.edit().putBoolean("setupDone",true).apply();if(prefs.getBoolean("reminder",true))scheduleReminder();screen="Beranda";render();});
    }

    private void progressIndicator(int activeStep){
        LinearLayout progress=row(content);
        LinearLayout.LayoutParams progressParams=(LinearLayout.LayoutParams)progress.getLayoutParams();
        progressParams.topMargin=dp(12);
        progressParams.bottomMargin=dp(18);
        progress.setLayoutParams(progressParams);
        for(int step=1;step<=2;step++){
            View segment=new View(this);
            segment.setBackground(shape(step<=activeStep?TEAL:0xffdfe5ec,dp(8)));
            LinearLayout.LayoutParams segmentParams=new LinearLayout.LayoutParams(0,dp(5),1);
            if(step==2)segmentParams.leftMargin=dp(6);
            progress.addView(segment,segmentParams);
        }
    }
    private void home(){
        header();
        text(content,new SimpleDateFormat("EEEE, d MMM",new Locale("id","ID")).format(new Date()),13,MUTED,false);
        cardTitle("ACUAN BELANJA HARI INI");
        long remaining=dailyPoolRemaining();
        long daily=Math.max(0,remaining/daysToNextPayout());
        TextView dailyAmount=text(content,money(daily),31,TEAL,true);
        dailyAmount.setOnClickListener(v->{screen="Proyeksi";render();});
        text(content,money(remaining)+" tersisa · "+daysToNextPayout()+" hari dalam periode ini",13,MUTED,false);

        LinearLayout budgetCard=card(content);
        LinearLayout budgetHeading=row(budgetCard);
        text(budgetHeading,"Anggaran 50 / 30 / 20",16,DARK,true);
        for(int i=0;i<3;i++){
            long part=budget()*share(i)/100;
            budgetRow(budgetCard,GROUPS[i],groupSpent(GROUPS[i]),part,COLORS[i]);
        }

        button(content,"＋ Catat Cepat",()->{screen="Transaksi";render();});

        LinearLayout recentCard=card(content);
        LinearLayout recentHeading=row(recentCard);
        text(recentHeading,"Transaksi terbaru",16,DARK,true);
        View spacer=new View(this);
        recentHeading.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
        pill(recentHeading,"Lihat semua",()->{screen="Catat";render();},false);
        addTransactionRows(recentCard,3);
    }

    private void header(){
        LinearLayout r=row(content);
        TextView mark=makeText("▣",20,Color.WHITE,true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(shape(TEAL,dp(12)));
        r.addView(mark,new LinearLayout.LayoutParams(dp(40),dp(40)));
        TextView brand=text(r,"KhasKu",19,DARK,true);
        brand.setPadding(dp(10),0,0,0);
        View spacer=new View(this);
        r.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
        TextView user=makeText("●",18,TEAL,true);
        user.setGravity(Gravity.CENTER);
        r.addView(user,new LinearLayout.LayoutParams(dp(36),dp(36)));
        user.setOnClickListener(v->{screen="Anggaran";render();});
    }

    private LinearLayout card(LinearLayout parent){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16),dp(14),dp(16),dp(14));
        box.setBackground(shape(Color.WHITE,dp(18)));
        LinearLayout.LayoutParams lp=space(-2);
        lp.topMargin=dp(8);
        lp.bottomMargin=dp(8);
        parent.addView(box,lp);
        return box;
    }

    private void budgetRow(LinearLayout parent,String label,long used,long limit,int color){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams boxParams=space(57);
        parent.addView(box,boxParams);
        LinearLayout r=row(box);
        TextView dot=text(r,"●",12,color,true);
        dot.setPadding(0,0,dp(7),0);
        text(r,label,14,DARK,true);
        View spacer=new View(this);
        r.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
        text(r,money(used)+" / "+money(limit),11,MUTED,false);
        FrameLayout bar=new FrameLayout(this);
        bar.setBackground(shape(0xffe5e9f4,99));
        LinearLayout.LayoutParams barParams=space(6);
        barParams.topMargin=dp(4);
        box.addView(bar,barParams);
        View fill=new View(this);
        fill.setBackground(shape(color,99));
        bar.addView(fill,new FrameLayout.LayoutParams(0,-1,Gravity.START));
        bar.post(()->{
            double ratio=limit<=0?0:Math.min(1d,(double)used/limit);
            FrameLayout.LayoutParams params=(FrameLayout.LayoutParams)fill.getLayoutParams();
            params.width=(int)(bar.getWidth()*ratio);
            fill.setLayoutParams(params);
        });
    }
    private void history(){
        header();
        text(content,"Riwayat Transaksi",23,DARK,true);
        text(content,"Bulan ini · "+transactions().length()+" transaksi tercatat",13,MUTED,false);
        LinearLayout filters=row(content);
        pill(filters,"Semua",()->{historyFilter="Semua";render();},historyFilter.equals("Semua"));
        LinearLayout groupFilters=row(content);
        for(String group:GROUPS)pill(groupFilters,group,()->{historyFilter=group;render();},historyFilter.equals(group));
        LinearLayout moreFilters=row(content);
        pill(moreFilters,"Jejak Recehan",()->{historyFilter="Recehan";render();},historyFilter.equals("Recehan"));
        pill(moreFilters,"Pilih kategori",()->new AlertDialog.Builder(this).setTitle("Pilih kategori").setItems(CATEGORIES,(dialog,which)->{historyCategory=CATEGORIES[which];historyFilter="Kategori";render();}).show(),historyFilter.equals("Kategori"));
        LinearLayout totals=card(content);
        text(totals,"Total pengeluaran",12,MUTED,false);
        text(totals,money(spent()),20,DARK,true);
        addTransactionRows(content,0);
        button(content,"＋ Catat Cepat",()->{screen="Transaksi";render();});
    }

    private void summary(){
        header();
        text(content,"Ringkasan Mingguan",23,DARK,true);
        text(content,"Minggu ini · "+weekRange(),13,MUTED,false);
        LinearLayout stable=card(content);
        text(stable,"Pengeluaran stabil",15,TEAL,true);
        text(stable,"Belanja minggu ini",12,MUTED,false);
        text(stable,money(spentSince(weekStart())),24,DARK,true);
        text(stable,"dari "+money(budget())+" anggaran bulanan",12,MUTED,false);
        LinearLayout allocation=card(content);
        text(allocation,"Anggaran 50 / 30 / 20",16,DARK,true);
        for(int i=0;i<3;i++){
            long part=budget()*share(i)/100;
            budgetRow(allocation,GROUPS[i],groupSpentSince(GROUPS[i],periodStart()),part,COLORS[i]);
        }
        LinearLayout largest=card(content);
        text(largest,"Pengeluaran terbesar minggu ini",16,DARK,true);
        addTopCategoryRows(largest,weekStart());
        LinearLayout crumbs=card(content);
        text(crumbs,"Jejak Recehan",16,DARK,true);
        text(crumbs,money(crumbsSince(weekStart()))+" · "+crumbCountSince(weekStart())+" transaksi",14,TEAL,true);
        button(content,"Lihat detail Recehan",()->{screen="Recehan";render();});
    }

    private void recehan(){
        header();
        text(content,"Jejak Recehan",23,DARK,true);
        text(content,"Minggu ini · "+weekRange(),13,MUTED,false);
        LinearLayout total=card(content);
        text(total,"Pengeluaran mikro & receh",14,TEAL,true);
        text(total,crumbCountSince(weekStart())+" transaksi",12,MUTED,false);
        text(total,money(crumbsSince(weekStart())),29,0xffbd4317,true);
        LinearLayout estimate=card(content);
        text(estimate,"Estimasi 4 minggu",13,MUTED,false);
        text(estimate,money(crumbsSince(weekStart())*4),18,DARK,true);
        text(estimate,"Dengan pola serupa",12,MUTED,false);
        LinearLayout distribution=card(content);
        text(distribution,"Sebaran recehan terbesar",16,DARK,true);
        addTopCrumbRows(distribution,weekStart());
        text(content,"Transaksi Recehan",17,DARK,true);
        addTransactionRows(content,0);
        LinearLayout tip=card(content);
        text(tip,"Tips Anti-Bocor Halus",16,TEAL,true);
        text(tip,"Pantau pengeluaran kecil untuk memahami pola belanjamu.",13,MUTED,false);
    }

    private void saved(){
        TextView check=makeText("✓",32,Color.WHITE,true);
        check.setGravity(Gravity.CENTER);
        check.setBackground(shape(TEAL,dp(40)));
        LinearLayout.LayoutParams checkParams=new LinearLayout.LayoutParams(dp(72),dp(72));
        checkParams.gravity=Gravity.CENTER_HORIZONTAL;
        checkParams.topMargin=dp(30);
        content.addView(check,checkParams);
        TextView title=text(content,"Tersimpan",27,DARK,true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams=(LinearLayout.LayoutParams)title.getLayoutParams();
        titleParams.width=-1;
        title.setLayoutParams(titleParams);
        title.setPadding(0,dp(16),0,0);
        TextView amount=text(content,selectedCategory+" · "+selectedGroup+" · "+money(lastSavedAmount),15,MUTED,false);
        amount.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams amountParams=(LinearLayout.LayoutParams)amount.getLayoutParams();
        amountParams.width=-1;
        amount.setLayoutParams(amountParams);
        LinearLayout remaining=card(content);
        text(remaining,"Sisa jatah hari ini",14,MUTED,false);
        long daily=Math.max(0,dailyPoolRemaining()/daysToNextPayout());
        text(remaining,money(daily),30,TEAL,true);
        text(remaining,"Acuan dihitung ulang besok.",13,MUTED,false);
        LinearLayout actions=row(content);
        TextView details=makeText("Lihat detail",14,TEAL,true);
        details.setGravity(Gravity.CENTER);
        details.setBackground(shape(Color.WHITE,dp(14)));
        LinearLayout.LayoutParams detailParams=new LinearLayout.LayoutParams(0,dp(56),1);
        detailParams.rightMargin=dp(8);
        actions.addView(details,detailParams);
        details.setOnClickListener(v->{screen=lastSavedCrumbs?"Recehan":"Catat";render();});
        TextView done=makeText("Selesai",14,Color.WHITE,true);
        done.setGravity(Gravity.CENTER);
        done.setBackground(shape(TEAL,dp(14)));
        actions.addView(done,new LinearLayout.LayoutParams(0,dp(56),1));
        done.setOnClickListener(v->{screen="Beranda";render();});
    }

    private void forecast(){
        LinearLayout top=row(content);
        TextView back=makeText("‹",28,DARK,true);
        top.addView(back);
        back.setOnClickListener(v->{screen="Beranda";render();});
        text(top,"Acuan Belanja Harian",20,DARK,true);
        long remaining=dailyPoolRemaining();
        long daily=Math.max(0,remaining/daysToNextPayout());
        TextView amount=text(content,money(daily)+" / hari",28,TEAL,true);
        amount.setGravity(Gravity.CENTER);
        LinearLayout projection=card(content);
        text(projection,"Proyeksi sisa belanja",16,DARK,true);
        View chart=new View(this){
            private final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(android.graphics.Canvas canvas){
                super.onDraw(canvas);
                float left=dp(8),right=getWidth()-dp(8),top=dp(16),bottom=getHeight()-dp(16);
                paint.setColor(0xffe6e9f2);
                paint.setStrokeWidth(dp(1));
                canvas.drawLine(left,top,right,top,paint);
                canvas.drawLine(left,(top+bottom)/2,right,(top+bottom)/2,paint);
                paint.setColor(0xff94a3b8);
                paint.setStrokeWidth(dp(2));
                canvas.drawLine(left,top,right,bottom,paint);
            }
        };
        projection.addView(chart,space(150));
        text(projection,"Hari ini                         Akhir periode",12,MUTED,false);
        LinearLayout calculation=card(content);
        text(calculation,"Rincian perhitungan",15,MUTED,true);
        text(calculation,"Sisa belanja   "+money(remaining),16,DARK,true);
        text(calculation,"Sisa waktu     "+daysToNextPayout()+" hari",16,DARK,true);
        button(content,"＋ Catat Lagi",()->{screen="Transaksi";render();});
    }
    private void budgetSetup(boolean unused){text(content,"‹  Atur Anggaran",22,DARK,true);EditText amount=edit(String.valueOf(budget()),true);content.addView(amount,space(52));text(content,"Pembagian anggaran (%)",16,DARK,true);EditText[] shares=new EditText[3];for(int i=0;i<3;i++){rowCard(GROUPS[i],"Bagian dari anggaran bulanan",share(i)+"%",COLORS[i]);shares[i]=edit(String.valueOf(share(i)),true);content.addView(shares[i],space(48));}button(content,"Simpan Anggaran",()->{long n=parseAmount(amount.getText().toString());int[] values=new int[3];int total=0;for(int i=0;i<3;i++){long p=parseAmount(shares[i].getText().toString());if(p>100){toast("Persentase harus 0–100");return;}values[i]=(int)p;total+=values[i];}if(n<=0){toast("Masukkan jumlah yang valid");return;}if(total!=100){toast("Total pembagian harus 100%");return;}SharedPreferences.Editor e=prefs.edit().putLong("budget",n);for(int i=0;i<3;i++)e.putInt("share"+i,values[i]);e.apply();screen="Beranda";render();});}
    private void quickEntry(){
        LinearLayout top=row(content);
        TextView close=makeText("×",28,MUTED,true);
        top.addView(close);
        close.setOnClickListener(v->{drafts();screen="Beranda";render();});
        View titleSpacer=new View(this);
        top.addView(titleSpacer,new LinearLayout.LayoutParams(0,1,1));
        text(top,"Catat Cepat",23,DARK,true);
        View closeSpacer=new View(this);
        top.addView(closeSpacer,new LinearLayout.LayoutParams(0,1,1));
        text(content,"CATAT PENGELUARANMU",11,TEAL,true);
        text(content,"NOMINAL",13,MUTED,true);
        amountInput=edit(draftAmount,true);
        amountInput.setHint("Rp 0");
        amountInput.setTextSize(TypedValue.COMPLEX_UNIT_SP,30);
        content.addView(amountInput,space(64));
        LinearLayout category=card(content);
        text(category,"Kategori",12,MUTED,false);
        TextView selected=text(category,selectedCategory+"  ·  "+selectedGroup+"     ›",17,DARK,true);
        selected.setOnClickListener(v->{drafts();showCategoryDialog();});
        category.setOnClickListener(v->{drafts();showCategoryDialog();});
        text(content,"Detail (opsional)",14,DARK,true);
        noteInput=edit(draftNote,false);
        noteInput.setHint("Catatan transaksi");
        content.addView(noteInput,space(50));
        button(content,"✓ Simpan Transaksi",this::saveTransaction);
        text(content,"Tersimpan hanya di perangkat ini",12,MUTED,false);
    }

    private void showCategoryDialog(){
        LinearLayout body=new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20),dp(8),dp(20),dp(4));
        TextView groupLabel=makeText("POS ANGGARAN",12,MUTED,true);
        body.addView(groupLabel,space(32));
        Spinner groupSpinner=new Spinner(this);
        ArrayAdapter<String> groupAdapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,GROUPS);
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupSpinner.setAdapter(groupAdapter);
        groupSpinner.setSelection(Math.max(0,java.util.Arrays.asList(GROUPS).indexOf(selectedGroup)));
        body.addView(groupSpinner,space(48));
        TextView categoryLabel=makeText("KATEGORI TRANSAKSI",12,MUTED,true);
        LinearLayout.LayoutParams labelParams=space(32);
        labelParams.topMargin=dp(12);
        body.addView(categoryLabel,labelParams);
        Spinner categorySpinner=new Spinner(this);
        ArrayAdapter<String> categoryAdapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,categoriesForGroup(selectedGroup));
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);
        String[] initialCategories=categoriesForGroup(selectedGroup);
        int selectedIndex=Math.max(0,java.util.Arrays.asList(initialCategories).indexOf(selectedCategory));
        categorySpinner.setSelection(selectedIndex);
        body.addView(categorySpinner,space(48));
        CheckBox crumbs=new CheckBox(this);
        crumbs.setText("Masuk Jejak Recehan (opsional)");
        crumbs.setTextColor(DARK);
        crumbs.setChecked(selectedCrumbs);
        body.addView(crumbs,space(48));

        groupSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){
                String group=GROUPS[position];
                String[] options=categoriesForGroup(group);
                ArrayAdapter<String> adapter=new ArrayAdapter<>(MainActivity.this,android.R.layout.simple_spinner_item,options);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                categorySpinner.setAdapter(adapter);
                categorySpinner.setSelection(Math.max(0,java.util.Arrays.asList(options).indexOf(selectedCategory)));
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent){}
        });

        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Pilih kategori transaksi").setView(body)
                .setNegativeButton("Batal",null).setPositiveButton("Simpan",null).create();
        dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            selectedGroup=GROUPS[groupSpinner.getSelectedItemPosition()];
            selectedCategory=String.valueOf(categorySpinner.getSelectedItem());
            selectedCrumbs=crumbs.isChecked();
            drafts();
            dialog.dismiss();
            render();
        }));
        dialog.show();
    }

    private String[] categoriesForGroup(String group){
        if("Kebutuhan".equals(group))return new String[]{"Makan","Transportasi","Kuliah","Kos","Pulsa & Data","Parkir kampus"};
        if("Keinginan".equals(group))return new String[]{"Jalan-jalan","Belanja","Top-up gim","Kopi susu aren","Langganan musik"};
        return new String[]{"Tabungan"};
    }

    private void saveTransaction(){
        long amount=parseAmount(amountInput.getText().toString());
        if(amount<=0){toast("Masukkan nominal yang valid");return;}
        JSONArray all=transactions();
        JSONObject transaction=new JSONObject();
        try{
            transaction.put("amount",amount);
            transaction.put("category",selectedCategory);
            transaction.put("group",selectedGroup);
            transaction.put("note",noteInput.getText().toString().trim());
            transaction.put("date",System.currentTimeMillis());
            transaction.put("crumbs",selectedCrumbs);
            all.put(transaction);
        }catch(org.json.JSONException e){
            android.util.Log.e("KhasKu","Could not save transaction",e);
            toast("Transaksi gagal disimpan");
            return;
        }
        prefs.edit().putString("transactions",all.toString()).apply();
        lastSavedAmount=amount;
        lastSavedCrumbs=selectedCrumbs;
        selectedCrumbs=false;
        draftAmount="";
        draftNote="";
        screen="Tersimpan";
        render();
    }

    private void addTransactionRows(LinearLayout parent,int limit){
        JSONArray all=transactions();
        if(all.length()==0){text(parent,"Belum ada transaksi",14,MUTED,false);return;}
        int shown=0;
        String today=new SimpleDateFormat("yyyyMMdd",Locale.ROOT).format(new Date());
        for(int i=all.length()-1;i>=0&&(limit==0||shown<limit);i--){
            JSONObject o=all.optJSONObject(i);
            if(o==null)continue;
            String cat=o.optString("category","Lainnya");
            if(screen.equals("Recehan")&&(!o.optBoolean("crumbs",false)||o.optLong("date")<weekStart()))continue;
            if(screen.equals("Catat")){
                if(GROUPS[0].equals(historyFilter)||GROUPS[1].equals(historyFilter)||GROUPS[2].equals(historyFilter)){
                    if(!historyFilter.equals(o.optString("group")))continue;
                }
                if(historyFilter.equals("Recehan")&&(!o.optBoolean("crumbs",false)||o.optLong("date")<weekStart()))continue;
                if(historyFilter.equals("Kategori")&&!historyCategory.equals(cat))continue;
            }
            if(limit>0&&o.optLong("date")<periodStart())continue;
            String note=o.optString("note","");
            LinearLayout r=row(parent);
            TextView icon=makeText("●",18,TEAL,true);
            icon.setPadding(dp(4),dp(4),dp(12),dp(4));
            r.addView(icon);
            LinearLayout name=new LinearLayout(this);
            name.setOrientation(LinearLayout.VERTICAL);
            r.addView(name,new LinearLayout.LayoutParams(0,-2,1));
            text(name,note.isEmpty()?cat:note,14,DARK,true);
            text(name,cat+" · "+new SimpleDateFormat("d MMM · HH:mm",new Locale("id","ID")).format(new Date(o.optLong("date"))),11,MUTED,false);
            text(r,"−"+money(o.optLong("amount")),14,DARK,true);
            r.setPadding(0,dp(10),0,dp(10));
            shown++;
        }
        if(shown==0)text(parent,"Tidak ada transaksi untuk filter ini",14,MUTED,false);
    }

    private void addTopCategoryRows(LinearLayout parent,long from){
        long[] totals=new long[CATEGORIES.length];
        int[] order=new int[CATEGORIES.length];
        for(int i=0;i<order.length;i++)order[i]=i;
        JSONArray all=transactions();
        for(int i=0;i<all.length();i++){
            JSONObject o=all.optJSONObject(i);
            if(o==null||o.optLong("date")<from)continue;
            for(int j=0;j<CATEGORIES.length;j++)if(CATEGORIES[j].equals(o.optString("category")))totals[j]+=o.optLong("amount");
        }
        for(int i=0;i<order.length;i++)for(int j=i+1;j<order.length;j++)if(totals[order[j]]>totals[order[i]]){int n=order[i];order[i]=order[j];order[j]=n;}
        int count=0;
        for(int idx:order){
            if(count>=3||totals[idx]==0)break;
            rowCard(parent,CATEGORIES[idx],"Pengeluaran",money(totals[idx]),COLORS[count%COLORS.length]);
            count++;
        }
        if(count==0)text(parent,"Belum ada transaksi di periode ini.",14,MUTED,false);
    }
    private long groupSpent(String group){return groupSpentSince(group,periodStart());}
    private long groupSpentSince(String group,long from){long sum=0;JSONArray a=transactions();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&group.equals(o.optString("group"))&&o.optLong("date")>=from)sum+=o.optLong("amount");}return sum;}
    private String weekRange(){
        Calendar start=Calendar.getInstance();
        start.setTimeInMillis(weekStart());
        Calendar end=(Calendar)start.clone();
        end.add(Calendar.DAY_OF_YEAR,6);
        Locale locale=new Locale("id","ID");
        SimpleDateFormat startDay=new SimpleDateFormat("d",locale);
        SimpleDateFormat startDate=new SimpleDateFormat("d MMM",locale);
        SimpleDateFormat endDate=new SimpleDateFormat("d MMM",locale);
        boolean sameMonth=start.get(Calendar.MONTH)==end.get(Calendar.MONTH);
        return (sameMonth?startDay.format(start.getTime()):startDate.format(start.getTime()))+"–"+endDate.format(end.getTime());
    }
    private long dailyPoolRemaining(){long dailyPool=budget()*(100-share(2))/100;return Math.max(0,dailyPool-groupSpentSince(GROUPS[0],periodStart())-groupSpentSince(GROUPS[1],periodStart()));}
    private long crumbsSince(long from){long sum=0;JSONArray a=transactions();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&o.optBoolean("crumbs",false)&&o.optLong("date")>=from)sum+=o.optLong("amount");}return sum;}
    private int crumbCountSince(long from){int count=0;JSONArray a=transactions();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&o.optBoolean("crumbs",false)&&o.optLong("date")>=from)count++;}return count;}
    private void addTopCrumbRows(LinearLayout parent,long from){
        long[] totals=new long[CATEGORIES.length];
        JSONArray all=transactions();
        for(int i=0;i<all.length();i++){
            JSONObject o=all.optJSONObject(i);
            if(o==null||!o.optBoolean("crumbs",false)||o.optLong("date")<from)continue;
            for(int j=0;j<CATEGORIES.length;j++)if(CATEGORIES[j].equals(o.optString("category")))totals[j]+=o.optLong("amount");
        }
        int[] order=new int[CATEGORIES.length];
        for(int i=0;i<order.length;i++)order[i]=i;
        for(int i=0;i<order.length;i++)for(int j=i+1;j<order.length;j++)if(totals[order[j]]>totals[order[i]]){int n=order[i];order[i]=order[j];order[j]=n;}
        int count=0;
        for(int idx:order){
            if(count>=4||totals[idx]==0)break;
            rowCard(parent,CATEGORIES[idx],"Jejak Recehan",money(totals[idx]),COLORS[count%COLORS.length]);
            count++;
        }
        if(count==0)text(parent,"Belum ada recehan tercatat minggu ini.",14,MUTED,false);
    }
    private int activeDaysThisWeek(){java.util.HashSet<String> days=new java.util.HashSet<>();JSONArray a=transactions();SimpleDateFormat f=new SimpleDateFormat("yyyyMMdd",Locale.ROOT);long start=weekStart();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&o.optLong("date")>=start)days.add(f.format(new Date(o.optLong("date"))));}return days.size();}
    private int daysToNextPayout(){Calendar today=Calendar.getInstance();today.set(Calendar.HOUR_OF_DAY,0);today.set(Calendar.MINUTE,0);today.set(Calendar.SECOND,0);today.set(Calendar.MILLISECOND,0);int payday=prefs.getInt("payoutDay",1);Calendar next=(Calendar)today.clone();next.set(Calendar.DAY_OF_MONTH,1);int effectiveDay=Math.min(payday,next.getActualMaximum(Calendar.DAY_OF_MONTH));if(today.get(Calendar.DAY_OF_MONTH)>=effectiveDay)next.add(Calendar.MONTH,1);next.set(Calendar.DAY_OF_MONTH,Math.min(payday,next.getActualMaximum(Calendar.DAY_OF_MONTH)));return Math.max(1,(int)((next.getTimeInMillis()-today.getTimeInMillis())/86400000L));}
    private void bottomNav(){LinearLayout nav=new LinearLayout(this);nav.setBackgroundColor(Color.WHITE);nav.setElevation(dp(8));page.addView(nav,new LinearLayout.LayoutParams(-1,dp(62)));String[] labels={"Beranda","Catat","Recehan","Ringkasan"};String[] icons={"⌂","≡","Ⓢ","⌁"};for(int i=0;i<labels.length;i++){String s=labels[i];TextView t=makeText(icons[i]+"\n"+s,11,screen.equals(s)?TEAL:MUTED,screen.equals(s));t.setGravity(Gravity.CENTER);nav.addView(t,new LinearLayout.LayoutParams(0,-1,1));t.setOnClickListener(v->{screen=s;render();});}}
    private void cardTitle(String s){TextView t=text(content,s,16,DARK,true);t.setPadding(0,dp(13),0,dp(7));}
    private void rowCard(String title,String sub,String value,int color){rowCard(content,title,sub,value,color);}
    private void rowCard(LinearLayout parent,String title,String sub,String value,int color){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(dp(12),dp(12),dp(12),dp(12));r.setBackground(shape(0xfff8f9fc,dp(12)));TextView dot=text(r,"●",14,color,true);dot.setPadding(0,0,dp(9),0);LinearLayout copy=new LinearLayout(this);copy.setOrientation(1);r.addView(copy,new LinearLayout.LayoutParams(0,-2,1));text(copy,title,14,DARK,true);text(copy,sub,11,MUTED,false);text(r,value,13,DARK,true);LinearLayout.LayoutParams lp=space(72);lp.bottomMargin=dp(7);parent.addView(r,lp);}
    private void button(LinearLayout parent,String label,Runnable action){TextView b=makeText(label,14,Color.WHITE,true);b.setGravity(Gravity.CENTER);b.setBackground(shape(TEAL,dp(12)));b.setPadding(dp(12),dp(14),dp(12),dp(14));LinearLayout.LayoutParams lp=space(52);lp.topMargin=dp(8);parent.addView(b,lp);b.setOnClickListener(v->action.run());}
    private void pill(LinearLayout parent,String label,Runnable action,boolean active){TextView t=makeText(label,12,active?Color.WHITE:TEAL,true);t.setGravity(Gravity.CENTER);t.setBackground(shape(active?TEAL:0xffe7f3f1,dp(24)));t.setPadding(dp(12),dp(9),dp(12),dp(9));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.setMargins(0,dp(5),dp(7),dp(5));parent.addView(t,lp);t.setOnClickListener(v->action.run());}
    private EditText edit(String val,boolean numeric){EditText e=new EditText(this);e.setSingleLine(true);e.setText(val);e.setTextColor(DARK);e.setTextSize(20);e.setPadding(dp(12),0,dp(12),0);e.setBackground(shape(Color.WHITE,dp(12)));if(numeric)e.setInputType(InputType.TYPE_CLASS_NUMBER);return e;}
    private TextView text(LinearLayout parent,String value,int size,int color,boolean bold){TextView t=makeText(value,size,color,bold);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.bottomMargin=dp(5);parent.addView(t,lp);return t;}
    private TextView makeText(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
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
