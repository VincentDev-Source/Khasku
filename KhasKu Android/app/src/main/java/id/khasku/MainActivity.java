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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int TEAL=0xff147c72, DARK=0xff172033, BG=0xfff6f8fb, MUTED=0xff718087;
    private static final String DEFAULT_WALLET_ID="default";
    private static final String[] GROUPS={"Kebutuhan","Keinginan","Dana Darurat"};
    private static final String[] CATEGORIES={"Makan","Transportasi","Kuliah","Kos","Pulsa & Data","Jalan-jalan","Belanja","Top-up gim","Kopi susu aren","Parkir kampus","Langganan musik","Dana darurat"};
    private static final int[] COLORS={0xff15977f,0xffec8650,0xff665de8};
    private SharedPreferences prefs;
    private LinearLayout page, content;
    private String screen="Beranda", selectedGroup="Kebutuhan", selectedCategory="Makan";
    private String selectedWalletId=DEFAULT_WALLET_ID;
    private EditText amountInput, noteInput;
    private int period=0;
    private String draftAmount="", draftNote="";
    private String historyFilter="Semua", historyCategory="";
    private int editingTransactionIndex=-1;
    private boolean selectedCrumbs;
    private boolean lastSavedCrumbs;
    private long lastSavedAmount;
    private int crumbsWeekOffset;

    @Override public void onCreate(Bundle state){super.onCreate(state); getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(Color.WHITE); getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR); prefs=getSharedPreferences("khasku_local",MODE_PRIVATE); ensureLedgerData(); selectedWalletId=prefs.getString("activeWallet",DEFAULT_WALLET_ID); render();}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private String money(long n){return "Rp"+NumberFormat.getInstance(new Locale("id","ID")).format(n);}
    private long budget(){return prefs.getLong("budget",0);}
    private long spent(){return spentSince(periodStart());}
    private long spentSince(long from){long sum=0;JSONArray a=transactions();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&o.optLong("date")>=from)sum+=o.optLong("amount");}return sum;}
    private long periodStart(){Calendar c=Calendar.getInstance();int today=c.get(Calendar.DAY_OF_MONTH),day=prefs.getInt("payoutDay",1);c.set(Calendar.DAY_OF_MONTH,1);int effectiveDay=Math.min(day,c.getActualMaximum(Calendar.DAY_OF_MONTH));if(today<effectiveDay)c.add(Calendar.MONTH,-1);c.set(Calendar.DAY_OF_MONTH,Math.min(day,c.getActualMaximum(Calendar.DAY_OF_MONTH)));c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c.getTimeInMillis();}
    private long weekStart(){Calendar c=Calendar.getInstance();c.set(Calendar.DAY_OF_WEEK,c.getFirstDayOfWeek());c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c.getTimeInMillis();}
    private int share(int index){return prefs.getInt("share"+index,new int[]{50,30,20}[index]);}
    private String allocationTitle(){return "Alokasi "+share(0)+" / "+share(1)+" / "+share(2);}
    private JSONArray transactions(){try{return new JSONArray(prefs.getString("transactions","[]"));}catch(Exception e){return new JSONArray();}}
    private JSONArray wallets(){try{return new JSONArray(prefs.getString("wallets","[]"));}catch(org.json.JSONException e){android.util.Log.e("KhasKu","Could not read wallets",e);return new JSONArray();}}
    private JSONArray customCategories(){try{return new JSONArray(prefs.getString("customCategories","[]"));}catch(org.json.JSONException e){android.util.Log.e("KhasKu","Could not read custom categories",e);return new JSONArray();}}
    private void ensureLedgerData(){
        JSONArray walletList=wallets();
        if(walletList.length()==0){
            JSONObject wallet=new JSONObject();
            try{wallet.put("id",DEFAULT_WALLET_ID);wallet.put("name","Dompet utama");walletList.put(wallet);}
            catch(org.json.JSONException e){android.util.Log.e("KhasKu","Could not create default wallet",e);}
            prefs.edit().putString("wallets",walletList.toString()).putString("activeWallet",DEFAULT_WALLET_ID).apply();
        }
        JSONArray all=transactions();
        boolean migrated=false;
        for(int i=0;i<all.length();i++){
            JSONObject transaction=all.optJSONObject(i);
            if(transaction!=null&&!transaction.has("walletId")){
                try{transaction.put("walletId",DEFAULT_WALLET_ID);all.put(i,transaction);migrated=true;}
                catch(org.json.JSONException e){android.util.Log.e("KhasKu","Could not migrate transaction wallet",e);}
            }
        }
        if(migrated)prefs.edit().putString("transactions",all.toString()).apply();
        String activeWallet=prefs.getString("activeWallet",DEFAULT_WALLET_ID);
        boolean activeExists=false;
        for(int i=0;i<walletList.length();i++){JSONObject wallet=walletList.optJSONObject(i);if(wallet!=null&&activeWallet.equals(wallet.optString("id"))){activeExists=true;break;}}
        if(!activeExists)prefs.edit().putString("activeWallet",DEFAULT_WALLET_ID).apply();
    }
    private String walletName(String id){
        JSONArray all=wallets();
        for(int i=0;i<all.length();i++){JSONObject wallet=all.optJSONObject(i);if(wallet!=null&&id.equals(wallet.optString("id")))return wallet.optString("name","Dompet utama");}
        return "Dompet utama";
    }
    private long walletSpent(String id){
        long total=0;
        JSONArray all=transactions();
        for(int i=0;i<all.length();i++){JSONObject transaction=all.optJSONObject(i);if(transaction!=null&&id.equals(transaction.optString("walletId",DEFAULT_WALLET_ID)))total+=transaction.optLong("amount");}
        return total;
    }
    private String normalizeGroup(String group){return "Tabungan".equals(group)?GROUPS[2]:group;}
    private String defaultCategoryGroup(String category){
        if("Tabungan".equals(category)||"Dana darurat".equals(category))return GROUPS[2];
        if("Makan".equals(category)||"Transportasi".equals(category)||"Kuliah".equals(category)||"Kos".equals(category)||"Pulsa & Data".equals(category)||"Parkir kampus".equals(category))return GROUPS[0];
        if("Jalan-jalan".equals(category)||"Belanja".equals(category)||"Top-up gim".equals(category)||"Kopi susu aren".equals(category)||"Langganan musik".equals(category))return GROUPS[1];
        return null;
    }
    private String categoryGroup(String category){
        String builtIn=defaultCategoryGroup(category);
        if(builtIn!=null)return builtIn;
        JSONArray all=customCategories();
        for(int i=0;i<all.length();i++){JSONObject item=all.optJSONObject(i);if(item!=null&&category.equals(item.optString("name")))return normalizeGroup(item.optString("group",GROUPS[0]));}
        return GROUPS[0];
    }
    private String[] categoriesForGroup(String group){
        ArrayList<String> result=new ArrayList<>();
        for(String category:CATEGORIES)if(normalizeGroup(group).equals(defaultCategoryGroup(category)))result.add(category);
        if(GROUPS[2].equals(normalizeGroup(group))&&hasTransactionsForCategory("Tabungan"))result.add("Tabungan");
        JSONArray custom=customCategories();
        for(int i=0;i<custom.length();i++){JSONObject item=custom.optJSONObject(i);if(item!=null&&normalizeGroup(group).equals(normalizeGroup(item.optString("group",GROUPS[0]))))result.add(item.optString("name"));}
        return result.toArray(new String[0]);
    }
    private String[] categoriesForSelection(String group,String selected){
        String[] available=categoriesForGroup(group);
        for(String category:available)if(category.equals(selected))return available;
        String[] result=java.util.Arrays.copyOf(available,available.length+1);
        result[available.length]=selected;
        return result;
    }
    private String[] categoryNames(){
        LinkedHashSet<String> names=new LinkedHashSet<>();
        for(String category:CATEGORIES)names.add(category);
        JSONArray custom=customCategories();
        for(int i=0;i<custom.length();i++){JSONObject item=custom.optJSONObject(i);if(item!=null)names.add(item.optString("name"));}
        JSONArray all=transactions();
        for(int i=0;i<all.length();i++){JSONObject transaction=all.optJSONObject(i);if(transaction!=null)names.add(transaction.optString("category","Lainnya"));}
        return names.toArray(new String[0]);
    }
    private void render(){
        page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setBackgroundColor(BG); setContentView(page);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(20),dp(12),dp(20),dp(24)); scroll.addView(content);
        if(budget()==0) onboarding(); else if(screen.equals("Anggaran")) budgetSetup(false); else if(!prefs.getBoolean("setupDone",false)) onboarding2(); else switch(screen){case "Catat":history();break;case "Dompet":walletManager();break;case "Kategori":categoryManager();break;case "Recehan":recehan();break;case "Ringkasan":summary();break;case "Transaksi":quickEntry();break;case "Tersimpan":saved();break;case "Proyeksi":forecast();break;default:home();}
        if(budget()>0 && prefs.getBoolean("setupDone",false) && !screen.equals("Transaksi")&&!screen.equals("Tersimpan")&&!screen.equals("Proyeksi")&&!screen.equals("Anggaran")) bottomNav();
    }
    private void onboarding(){
        progressIndicator(1);
        text(content,"LANGKAH 1 DARI 2",12,MUTED,true); text(content,"Atur anggaran bulananmu",23,DARK,true); text(content,"Mulai dengan jumlah yang ingin kamu kelola bulan ini.",14,MUTED,false);
        EditText amount=edit("1500000",true); content.addView(amount,space(52));
        text(content,"Aturan 50/30/20",15,DARK,true); text(content,"Kebutuhan, keinginan, dan dana darurat",13,MUTED,false);
        for(int i=0;i<3;i++)rowCard(GROUPS[i],new String[]{"Makan, transportasi, kos","Jajan, hiburan, gaya hidup","Dana untuk kebutuhan tak terduga"}[i],new int[]{50,30,20}[i]+"%",COLORS[i]);
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
        pill(content,"Dompet: "+walletName(selectedWalletId),()->{screen="Dompet";render();},false);
        cardTitle("ACUAN BELANJA HARI INI");
        long remaining=dailyPoolRemaining();
        long daily=Math.max(0,remaining/daysToNextPayout());
        TextView dailyAmount=text(content,money(daily),31,TEAL,true);
        dailyAmount.setOnClickListener(v->{screen="Proyeksi";render();});
        text(content,money(remaining)+" tersisa · "+daysToNextPayout()+" hari dalam periode ini",13,MUTED,false);

        LinearLayout budgetCard=card(content);
        LinearLayout budgetHeading=row(budgetCard);
        text(budgetHeading,allocationTitle(),16,DARK,true);
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

    private void walletManager(){
        header();
        text(content,"Dompet",23,DARK,true);
        text(content,"Pilih dompet untuk transaksi atau kelola dompetmu.",13,MUTED,false);
        JSONArray all=wallets();
        for(int i=0;i<all.length();i++){
            JSONObject wallet=all.optJSONObject(i);
            if(wallet==null)continue;
            String id=wallet.optString("id",DEFAULT_WALLET_ID);
            LinearLayout item=card(content);
            LinearLayout heading=row(item);
            text(heading,wallet.optString("name","Dompet utama"),16,DARK,true);
            View spacer=new View(this);
            heading.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
            if(id.equals(selectedWalletId))pill(heading,"Aktif",()->{},true);
            text(item,money(walletSpent(id))+" pengeluaran tercatat",13,MUTED,false);
            LinearLayout actions=row(item);
            if(!id.equals(selectedWalletId))pill(actions,"Pakai",()->{selectedWalletId=id;prefs.edit().putString("activeWallet",id).apply();screen="Beranda";render();},false);
            pill(actions,"Ubah nama",()->showWalletNameDialog(id,wallet.optString("name","Dompet utama")),false);
            if(!DEFAULT_WALLET_ID.equals(id))pill(actions,"Hapus",()->deleteWallet(id),false);
        }
        button(content,"＋ Tambah Dompet",()->showWalletNameDialog(null,""));
        button(content,"Kelola Kategori",()->{screen="Kategori";render();});
    }

    private void showWalletNameDialog(String walletId,String currentName){
        EditText name=edit(currentName,false);
        name.setHint("Nama dompet");
        LinearLayout body=new LinearLayout(this);
        body.setPadding(dp(20),dp(8),dp(20),0);
        body.addView(name,space(52));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(walletId==null?"Tambah Dompet":"Ubah Nama Dompet").setView(body)
                .setNegativeButton("Batal",null).setPositiveButton("Simpan",null).create();
        dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String value=name.getText().toString().trim();
            if(value.isEmpty()){toast("Masukkan nama dompet");return;}
            JSONArray all=wallets();
            JSONObject target=null;
            for(int i=0;i<all.length();i++){
                JSONObject wallet=all.optJSONObject(i);
                if(wallet==null)continue;
                if(walletId!=null&&walletId.equals(wallet.optString("id"))){target=wallet;continue;}
                if(value.equalsIgnoreCase(wallet.optString("name"))){toast("Nama dompet sudah digunakan");return;}
            }
            if(target==null){
                target=new JSONObject();
                try{target.put("id",UUID.randomUUID().toString());target.put("name",value);all.put(target);}
                catch(org.json.JSONException e){android.util.Log.e("KhasKu","Could not create wallet",e);toast("Dompet gagal disimpan");return;}
                selectedWalletId=target.optString("id",DEFAULT_WALLET_ID);
                prefs.edit().putString("activeWallet",selectedWalletId).apply();
            }else{
                try{target.put("name",value);}
                catch(org.json.JSONException e){android.util.Log.e("KhasKu","Could not rename wallet",e);toast("Nama dompet gagal disimpan");return;}
            }
            prefs.edit().putString("wallets",all.toString()).apply();
            dialog.dismiss();
            render();
        }));
        dialog.show();
    }

    private void deleteWallet(String walletId){
        JSONArray entries=transactions();
        for(int i=0;i<entries.length();i++){
            JSONObject transaction=entries.optJSONObject(i);
            if(transaction!=null&&walletId.equals(transaction.optString("walletId",DEFAULT_WALLET_ID))){toast("Dompet yang memiliki transaksi tidak dapat dihapus");return;}
        }
        new AlertDialog.Builder(this).setTitle("Hapus dompet?")
                .setMessage("Dompet ini akan dihapus dari perangkat.")
                .setNegativeButton("Batal",null).setPositiveButton("Hapus",(dialog,which)->{
                    JSONArray all=wallets();
                    for(int i=all.length()-1;i>=0;i--){JSONObject wallet=all.optJSONObject(i);if(wallet!=null&&walletId.equals(wallet.optString("id")))all.remove(i);}
                    SharedPreferences.Editor editor=prefs.edit().putString("wallets",all.toString());
                    if(walletId.equals(selectedWalletId)){selectedWalletId=DEFAULT_WALLET_ID;editor.putString("activeWallet",DEFAULT_WALLET_ID);}
                    editor.apply();
                    render();
                }).show();
    }

    private void categoryManager(){
        header();
        text(content,"Kategori",23,DARK,true);
        text(content,"Tambah kategori ke kebutuhan, keinginan, atau dana darurat.",13,MUTED,false);
        for(String group:GROUPS){
            cardTitle(group);
            for(String category:categoriesForGroup(group)){
                LinearLayout item=card(content);
                LinearLayout line=row(item);
                text(line,category,15,DARK,true);
                View spacer=new View(this);
                line.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
                text(line,group,12,MUTED,false);
                if(isCustomCategory(category)){
                    LinearLayout actions=row(item);
                    pill(actions,"Ubah",()->showCustomCategoryDialog(category),false);
                    pill(actions,"Hapus",()->removeCustomCategory(category),false);
                }
            }
        }
        button(content,"＋ Tambah Kategori",this::showCustomCategoryDialog);
        button(content,"Kembali ke Dompet",()->{screen="Dompet";render();});
    }

    private boolean isCustomCategory(String name){
        JSONArray all=customCategories();
        for(int i=0;i<all.length();i++){JSONObject item=all.optJSONObject(i);if(item!=null&&name.equals(item.optString("name")))return true;}
        return false;
    }

    private void showCustomCategoryDialog(){
        showCustomCategoryDialog(null);
    }

    private void showCustomCategoryDialog(String editingName){
        LinearLayout body=new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20),dp(8),dp(20),0);
        EditText name=edit(editingName==null?"":editingName,false);
        name.setHint("Nama kategori");
        name.setEnabled(editingName==null||!hasTransactionsForCategory(editingName));
        body.addView(name,space(52));
        Spinner groupSpinner=new Spinner(this);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,GROUPS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupSpinner.setAdapter(adapter);
        if(editingName!=null)groupSpinner.setSelection(Math.max(0,java.util.Arrays.asList(GROUPS).indexOf(categoryGroup(editingName))));
        body.addView(groupSpinner,space(48));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(editingName==null?"Tambah Kategori":"Ubah Kategori").setView(body)
                .setNegativeButton("Batal",null).setPositiveButton("Simpan",null).create();
        dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String value=name.getText().toString().trim();
            if(value.isEmpty()){toast("Masukkan nama kategori");return;}
            JSONArray all=customCategories();
            JSONObject item=null;
            int itemIndex=-1;
            for(int i=0;i<all.length();i++){
                JSONObject candidate=all.optJSONObject(i);
                if(candidate==null)continue;
                if(editingName!=null&&editingName.equals(candidate.optString("name"))){item=candidate;itemIndex=i;continue;}
                if(value.equalsIgnoreCase(candidate.optString("name"))){toast("Kategori sudah ada");return;}
            }
            for(String existing:categoryNames())if(!existing.equals(editingName)&&existing.equalsIgnoreCase(value)){toast("Kategori sudah ada");return;}
            if(item==null)item=new JSONObject();
            try{
                item.put("name",value);
                item.put("group",GROUPS[groupSpinner.getSelectedItemPosition()]);
                if(itemIndex<0)all.put(item);else all.put(itemIndex,item);
            }
            catch(org.json.JSONException e){android.util.Log.e("KhasKu","Could not create category",e);toast("Kategori gagal disimpan");return;}
            prefs.edit().putString("customCategories",all.toString()).apply();
            dialog.dismiss();
            render();
        }));
        dialog.show();
    }

    private boolean hasTransactionsForCategory(String name){
        JSONArray entries=transactions();
        for(int i=0;i<entries.length();i++){JSONObject transaction=entries.optJSONObject(i);if(transaction!=null&&name.equals(transaction.optString("category")))return true;}
        return false;
    }

    private void removeCustomCategory(String name){
        if(hasTransactionsForCategory(name)){toast("Kategori yang memiliki transaksi tidak dapat dihapus");return;}
        JSONArray all=customCategories();
        JSONArray remaining=new JSONArray();
        for(int i=0;i<all.length();i++){
            JSONObject item=all.optJSONObject(i);
            if(item!=null&&!name.equals(item.optString("name")))remaining.put(item);
        }
        prefs.edit().putString("customCategories",remaining.toString()).apply();
        render();
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
        String[] availableCategories=categoryNames();
        pill(moreFilters,"Pilih kategori",()->new AlertDialog.Builder(this).setTitle("Pilih kategori").setItems(availableCategories,(dialog,which)->{historyCategory=availableCategories[which];historyFilter="Kategori";render();}).show(),historyFilter.equals("Kategori"));
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
        text(allocation,allocationTitle(),16,DARK,true);
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
        long from=crumbWeekStart();
        long to=crumbWeekEnd();
        LinearLayout titleRow=row(content);
        LinearLayout titleCopy=new LinearLayout(this);
        titleCopy.setOrientation(LinearLayout.VERTICAL);
        titleRow.addView(titleCopy,new LinearLayout.LayoutParams(0,-2,1));
        text(titleCopy,"Jejak Recehan",23,DARK,true);
        text(titleCopy,(crumbsWeekOffset==0?"Minggu ini · ":"Minggu lalu · ")+weekRange(from),13,MUTED,false);
        pill(titleRow,crumbsWeekOffset==0?"Minggu Ini  ⌄":"Minggu Lalu  ⌄",()->new AlertDialog.Builder(this)
                .setItems(new String[]{"Minggu Ini","Minggu Lalu"},(dialog,which)->{crumbsWeekOffset=which;render();}).show(),false);

        LinearLayout total=card(content);
        LinearLayout totalHeading=row(total);
        TextView tag=makeText("♧ Pengeluaran Mikro & Receh",12,0xffbd4317,true);
        tag.setPadding(dp(9),dp(5),dp(9),dp(5));
        tag.setBackground(shape(0xfffff1c2,dp(18)));
        totalHeading.addView(tag);
        View spacer=new View(this);
        totalHeading.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
        text(totalHeading,crumbCountInRange(from,to)+" Transaksi",13,MUTED,false);
        text(total,"Total recehan "+(crumbsWeekOffset==0?"minggu ini":"minggu lalu"),13,MUTED,false);
        text(total,money(crumbsInRange(from,to)),29,0xffbd4317,true);

        LinearLayout estimate=new LinearLayout(this);
        estimate.setGravity(Gravity.CENTER_VERTICAL);
        estimate.setPadding(dp(12),dp(10),dp(12),dp(10));
        estimate.setBackground(shape(0xfff1f0ff,dp(14)));
        LinearLayout.LayoutParams estimateParams=space(-2);
        estimateParams.topMargin=dp(4);
        total.addView(estimate,estimateParams);
        TextView estimateIcon=makeText("↗",17,0xffd69a16,true);
        estimateIcon.setGravity(Gravity.CENTER);
        estimateIcon.setBackground(shape(0xfffff1c2,dp(20)));
        estimate.addView(estimateIcon,new LinearLayout.LayoutParams(dp(32),dp(32)));
        LinearLayout estimateCopy=new LinearLayout(this);
        estimateCopy.setOrientation(LinearLayout.VERTICAL);
        estimateCopy.setPadding(dp(9),0,0,0);
        estimate.addView(estimateCopy);
        text(estimateCopy,"Estimasi 4 minggu: "+money(crumbsInRange(from,to)*4),13,MUTED,true);
        text(estimateCopy,"dengan pola serupa",12,MUTED,false);

        button(content,"⊕  Catat recehan sekarang",()->{selectedCrumbs=true;screen="Transaksi";render();});

        LinearLayout distribution=card(content);
        LinearLayout distributionHeading=row(distribution);
        text(distributionHeading,"◴  Sebaran Recehan Terbesar",15,DARK,true);
        View distributionSpacer=new View(this);
        distributionHeading.addView(distributionSpacer,new LinearLayout.LayoutParams(0,1,1));
        text(distributionHeading,crumbsWeekOffset==0?"Minggu ini":"Minggu lalu",12,MUTED,false);
        long[] buckets=crumbBuckets(from,to);
        addCrumbDistribution(distribution,buckets);

        addCrumbTransactions(content,from,to);

        LinearLayout tip=card(content);
        LinearLayout tipHeading=row(tip);
        TextView bulb=makeText("♧",18,TEAL,true);
        bulb.setGravity(Gravity.CENTER);
        bulb.setBackground(shape(Color.WHITE,dp(20)));
        tipHeading.addView(bulb,new LinearLayout.LayoutParams(dp(36),dp(36)));
        TextView tipTitle=text(tipHeading,"Tips Anti-Bocor Halus",15,TEAL,true);
        tipTitle.setPadding(dp(10),0,0,0);
        text(tip,"Pantau pengeluaran kecil untuk memahami pola belanjamu.",13,MUTED,false);
        tip.setBackground(shape(0xffe4f3f0,dp(18)));
    }

    private long[] crumbBuckets(long from,long to){
        long[] totals=new long[5];
        JSONArray all=transactions();
        for(int i=0;i<all.length();i++){
            JSONObject o=all.optJSONObject(i);
            if(o==null||!o.optBoolean("crumbs",false)||o.optLong("date")<from||o.optLong("date")>=to)continue;
            totals[crumbBucketIndex(o.optString("category"))]+=o.optLong("amount");
        }
        return totals;
    }

    private int crumbBucketIndex(String category){
        if("Top-up gim".equals(category))return 0;
        if("Kopi susu aren".equals(category)||"Jalan-jalan".equals(category)||"Belanja".equals(category))return 1;
        if("Langganan musik".equals(category))return 2;
        if("Parkir kampus".equals(category))return 3;
        return 4;
    }

    private void addCrumbDistribution(LinearLayout parent,long[] totals){
        String[] labels={"Top-up gim","Kopi / Jajan","Langganan","Parkir","Lainnya"};
        int[] colors={0xffbd4317,0xffff7043,0xff554be8,TEAL,0xff788494};
        long sum=0;
        for(long total:totals)sum+=total;
        LinearLayout bar=new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackground(shape(0xffe6e9f2,dp(8)));
        bar.setClipToOutline(true);
        LinearLayout.LayoutParams barParams=space(7);
        barParams.topMargin=dp(4);
        barParams.bottomMargin=dp(12);
        parent.addView(bar,barParams);
        if(sum>0){
            for(int i=0;i<totals.length;i++){
                if(totals[i]==0)continue;
                View segment=new View(this);
                segment.setBackgroundColor(colors[i]);
                bar.addView(segment,new LinearLayout.LayoutParams(0,-1,(float)totals[i]));
            }
        }
        LinearLayout legendRow=null;
        for(int i=0;i<labels.length;i++){
            if(totals[i]==0)continue;
            if(legendRow==null||legendRow.getChildCount()==2)legendRow=row(parent);
            LinearLayout cell=new LinearLayout(this);
            cell.setOrientation(LinearLayout.HORIZONTAL);
            cell.setGravity(Gravity.CENTER_VERTICAL);
            cell.setPadding(dp(8),dp(8),dp(8),dp(8));
            cell.setBackground(shape(0xfff1f0ff,dp(12)));
            TextView dot=makeText("●",12,colors[i],true);
            cell.addView(dot);
            TextView label=makeText(labels[i],12,DARK,false);
            LinearLayout.LayoutParams labelParams=new LinearLayout.LayoutParams(0,-2,1);
            labelParams.leftMargin=dp(6);
            cell.addView(label,labelParams);
            int pct=sum==0?0:(int)Math.round(totals[i]*100d/sum);
            cell.addView(makeText(pct+"%",12,DARK,false));
            LinearLayout.LayoutParams cellParams=new LinearLayout.LayoutParams(0,-2,1);
            cellParams.setMargins(0,dp(4),dp(5),dp(4));
            legendRow.addView(cell,cellParams);
        }
        if(legendRow!=null&&legendRow.getChildCount()==1)legendRow.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));
    }

    private void addCrumbTransactions(LinearLayout parent,long from,long to){
        JSONArray all=transactions();
        java.util.ArrayList<JSONObject> entries=new java.util.ArrayList<>();
        for(int i=all.length()-1;i>=0;i--){
            JSONObject o=all.optJSONObject(i);
            if(o!=null&&o.optBoolean("crumbs",false)&&o.optLong("date")>=from&&o.optLong("date")<to)entries.add(o);
        }
        if(entries.isEmpty()){
            text(parent,"Belum ada recehan tercatat minggu ini.",14,MUTED,false);
            return;
        }
        int position=0;
        while(position<entries.size()){
            long firstDate=entries.get(position).optLong("date");
            Calendar first=Calendar.getInstance();
            first.setTimeInMillis(firstDate);
            long dayTotal=0;
            int end=position;
            while(end<entries.size()){
                Calendar candidate=Calendar.getInstance();
                candidate.setTimeInMillis(entries.get(end).optLong("date"));
                if(first.get(Calendar.YEAR)!=candidate.get(Calendar.YEAR)||first.get(Calendar.DAY_OF_YEAR)!=candidate.get(Calendar.DAY_OF_YEAR))break;
                dayTotal+=entries.get(end).optLong("amount");
                end++;
            }
            LinearLayout dayHeading=row(parent);
            TextView date=text(dayHeading,transactionDayLabel(firstDate),13,MUTED,true);
            View spacer=new View(this);
            dayHeading.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
            text(dayHeading,"-"+money(dayTotal),13,0xffbd4317,true);
            for(int i=position;i<end;i++)addCrumbTransactionCard(parent,entries.get(i));
            position=end;
        }
    }

    private String transactionDayLabel(long timestamp){
        Calendar day=Calendar.getInstance();
        day.setTimeInMillis(timestamp);
        Calendar today=Calendar.getInstance();
        Calendar yesterday=(Calendar)today.clone();
        yesterday.add(Calendar.DAY_OF_YEAR,-1);
        if(day.get(Calendar.YEAR)==today.get(Calendar.YEAR)&&day.get(Calendar.DAY_OF_YEAR)==today.get(Calendar.DAY_OF_YEAR))return "Hari ini, "+new SimpleDateFormat("d MMM",new Locale("id","ID")).format(day.getTime());
        if(day.get(Calendar.YEAR)==yesterday.get(Calendar.YEAR)&&day.get(Calendar.DAY_OF_YEAR)==yesterday.get(Calendar.DAY_OF_YEAR))return "Kemarin, "+new SimpleDateFormat("d MMM",new Locale("id","ID")).format(day.getTime());
        return new SimpleDateFormat("EEEE, d MMM",new Locale("id","ID")).format(day.getTime());
    }

    private void addCrumbTransactionCard(LinearLayout parent,JSONObject transaction){
        String category=transaction.optString("category","Pengeluaran");
        int bucket=crumbBucketIndex(category);
        int[] iconBackgrounds={0xfffff2c5,0xffffe8dd,0xffe8e6ff,0xffe3f2ef,0xffe3f2ef};
        int[] iconColors={0xffbd4317,0xffbd5a35,0xff554be8,TEAL,MUTED};
        String iconValue=categoryIcon(category);
        LinearLayout item=card(parent);
        item.setPadding(dp(10),dp(9),dp(10),dp(9));
        LinearLayout row=row(item);
        TextView icon=makeText(iconValue,18,iconColors[bucket],true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(shape(iconBackgrounds[bucket],dp(12)));
        row.addView(icon,new LinearLayout.LayoutParams(dp(44),dp(44)));
        LinearLayout copy=new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(9),0,dp(5),0);
        row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout titleRow=row(copy);
        text(titleRow,category,14,DARK,true);
        TextView badge=makeText("Recehan",10,bucket==1?0xffbd4317:TEAL,true);
        badge.setPadding(dp(6),dp(3),dp(6),dp(3));
        badge.setBackground(shape(bucket==1?0xfffff1c2:0xffe4f3f0,dp(8)));
        LinearLayout.LayoutParams badgeParams=(LinearLayout.LayoutParams)badge.getLayoutParams();
        badgeParams.leftMargin=dp(6);
        badge.setLayoutParams(badgeParams);
        titleRow.addView(badge);
        String note=transaction.optString("note","").trim();
        String details=transaction.optString("group","")+" • "+new SimpleDateFormat("HH:mm",new Locale("id","ID")).format(new Date(transaction.optLong("date")));
        if(!note.isEmpty())details+=" • "+note;
        text(copy,details,11,MUTED,false);
        int amountColor="Parkir kampus".equals(category)?DARK:0xffbd4317;
        text(row,"-"+money(transaction.optLong("amount")),14,amountColor,true);
    }

    private String categoryIcon(String category){
        switch(category){
            case "Top-up gim":return "🎮";
            case "Kopi susu aren":return "☕";
            case "Parkir kampus":return "P";
            case "Langganan musik":return "♫";
            case "Makan":return "🍴";
            case "Transportasi":return "↔";
            case "Kuliah":return "▤";
            case "Kos":return "⌂";
            default:return "•";
        }
    }

    private long crumbWeekStart(){
        Calendar start=Calendar.getInstance();
        start.setTimeInMillis(weekStart());
        start.add(Calendar.WEEK_OF_YEAR,-crumbsWeekOffset);
        return start.getTimeInMillis();
    }

    private long crumbWeekEnd(){
        Calendar end=Calendar.getInstance();
        end.setTimeInMillis(crumbWeekStart());
        end.add(Calendar.WEEK_OF_YEAR,1);
        return end.getTimeInMillis();
    }

    private long crumbsInRange(long from,long to){
        long sum=0;
        JSONArray all=transactions();
        for(int i=0;i<all.length();i++){
            JSONObject o=all.optJSONObject(i);
            if(o!=null&&o.optBoolean("crumbs",false)&&o.optLong("date")>=from&&o.optLong("date")<to)sum+=o.optLong("amount");
        }
        return sum;
    }

    private int crumbCountInRange(long from,long to){
        int count=0;
        JSONArray all=transactions();
        for(int i=0;i<all.length();i++){
            JSONObject o=all.optJSONObject(i);
            if(o!=null&&o.optBoolean("crumbs",false)&&o.optLong("date")>=from&&o.optLong("date")<to)count++;
        }
        return count;
    }

    private String weekRange(long startMillis){
        Calendar start=Calendar.getInstance();
        start.setTimeInMillis(startMillis);
        Calendar end=(Calendar)start.clone();
        end.add(Calendar.DAY_OF_YEAR,6);
        Locale locale=new Locale("id","ID");
        SimpleDateFormat startDay=new SimpleDateFormat("d",locale);
        SimpleDateFormat startDate=new SimpleDateFormat("d MMM",locale);
        SimpleDateFormat endDate=new SimpleDateFormat("d MMM",locale);
        boolean sameMonth=start.get(Calendar.MONTH)==end.get(Calendar.MONTH);
        return (sameMonth?startDay.format(start.getTime()):startDate.format(start.getTime()))+"–"+endDate.format(end.getTime());
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
    private void budgetSetup(boolean unused){text(content,"‹  Atur Anggaran",22,DARK,true);EditText amount=edit(String.valueOf(budget()),true);content.addView(amount,space(52));text(content,"Pembagian anggaran (%)",16,DARK,true);EditText[] shares=new EditText[3];for(int i=0;i<3;i++){rowCard(GROUPS[i],"Bagian dari anggaran bulanan",share(i)+"%",COLORS[i]);shares[i]=edit(String.valueOf(share(i)),true);content.addView(shares[i],space(48));}button(content,"Simpan Anggaran",()->{long n=parseAmount(amount.getText().toString());int[] values=new int[3];int total=0;for(int i=0;i<3;i++){long p=parseAmount(shares[i].getText().toString());if(p>100){toast("Persentase harus 0–100");return;}values[i]=(int)p;total+=values[i];}if(n<=0){toast("Masukkan jumlah yang valid");return;}if(total!=100){toast("Total pembagian harus 100%");return;}SharedPreferences.Editor e=prefs.edit().putLong("budget",n);for(int i=0;i<3;i++)e.putInt("share"+i,values[i]);e.apply();screen="Beranda";render();});button(content,"Kelola Dompet",()->{screen="Dompet";render();});button(content,"Kelola Kategori",()->{screen="Kategori";render();});}
    private void quickEntry(){
        LinearLayout top=row(content);
        TextView close=makeText("×",28,MUTED,true);
        top.addView(close);
        close.setOnClickListener(v->{drafts();editingTransactionIndex=-1;screen="Catat";render();});
        View titleSpacer=new View(this);
        top.addView(titleSpacer,new LinearLayout.LayoutParams(0,1,1));
        text(top,editingTransactionIndex<0?"Catat Cepat":"Ubah Transaksi",23,DARK,true);
        View closeSpacer=new View(this);
        top.addView(closeSpacer,new LinearLayout.LayoutParams(0,1,1));
        text(content,"CATAT PENGELUARANMU",11,TEAL,true);
        text(content,"NOMINAL",13,MUTED,true);
        amountInput=edit(draftAmount,true);
        amountInput.setHint("Rp 0");
        amountInput.setTextSize(TypedValue.COMPLEX_UNIT_SP,30);
        content.addView(amountInput,space(64));
        pill(content,"Dompet: "+walletName(selectedWalletId),()->chooseWallet(),false);
        LinearLayout category=card(content);
        text(category,"Kategori",12,MUTED,false);
        TextView selected=text(category,selectedCategory+"  ·  "+selectedGroup+"     ›",17,DARK,true);
        selected.setOnClickListener(v->{drafts();showCategoryDialog();});
        category.setOnClickListener(v->{drafts();showCategoryDialog();});
        text(content,"Detail (opsional)",14,DARK,true);
        noteInput=edit(draftNote,false);
        noteInput.setHint("Catatan transaksi");
        content.addView(noteInput,space(50));
        button(content,editingTransactionIndex<0?"✓ Simpan Transaksi":"✓ Simpan Perubahan",this::saveTransaction);
        text(content,"Tersimpan hanya di perangkat ini",12,MUTED,false);
    }

    private void chooseWallet(){
        JSONArray all=wallets();
        String[] names=new String[all.length()];
        String[] ids=new String[all.length()];
        for(int i=0;i<all.length();i++){JSONObject wallet=all.optJSONObject(i);names[i]=wallet==null?"Dompet utama":wallet.optString("name","Dompet utama");ids[i]=wallet==null?DEFAULT_WALLET_ID:wallet.optString("id",DEFAULT_WALLET_ID);}
        new AlertDialog.Builder(this).setTitle("Pilih Dompet").setItems(names,(dialog,which)->{
            drafts();
            selectedWalletId=ids[which];
            prefs.edit().putString("activeWallet",selectedWalletId).apply();
            render();
        }).setNegativeButton("Batal",null).show();
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
        ArrayAdapter<String> categoryAdapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,categoriesForSelection(selectedGroup,selectedCategory));
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);
        String[] initialCategories=categoriesForSelection(selectedGroup,selectedCategory);
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
                String[] options=group.equals(selectedGroup)?categoriesForSelection(group,selectedCategory):categoriesForGroup(group);
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

    private void saveTransaction(){
        long amount=parseAmount(amountInput.getText().toString());
        if(amount<=0){toast("Masukkan nominal yang valid");return;}
        JSONArray all=transactions();
        JSONObject transaction=editingTransactionIndex>=0?all.optJSONObject(editingTransactionIndex):new JSONObject();
        if(transaction==null){toast("Transaksi tidak ditemukan");editingTransactionIndex=-1;screen="Catat";render();return;}
        boolean editing=editingTransactionIndex>=0;
        try{
            transaction.put("amount",amount);
            transaction.put("category",selectedCategory);
            transaction.put("group",selectedGroup);
            transaction.put("note",noteInput.getText().toString().trim());
            if(!editing)transaction.put("date",System.currentTimeMillis());
            transaction.put("crumbs",selectedCrumbs);
            transaction.put("walletId",selectedWalletId);
            if(editing)all.put(editingTransactionIndex,transaction);else all.put(transaction);
        }catch(org.json.JSONException e){
            android.util.Log.e("KhasKu","Could not save transaction",e);
            toast("Transaksi gagal disimpan");
            return;
        }
        prefs.edit().putString("transactions",all.toString()).apply();
        lastSavedAmount=amount;
        lastSavedCrumbs=selectedCrumbs;
        editingTransactionIndex=-1;
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
                    if(!historyFilter.equals(normalizeGroup(o.optString("group"))))continue;
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
            text(name,cat+" · "+walletName(o.optString("walletId",DEFAULT_WALLET_ID))+" · "+new SimpleDateFormat("d MMM · HH:mm",new Locale("id","ID")).format(new Date(o.optLong("date"))),11,MUTED,false);
            text(r,"−"+money(o.optLong("amount")),14,DARK,true);
            TextView actions=makeText("⋮",22,MUTED,true);
            actions.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams actionsParams=new LinearLayout.LayoutParams(dp(32),dp(40));
            r.addView(actions,actionsParams);
            r.setPadding(0,dp(10),0,dp(10));
            int transactionIndex=i;
            r.setOnClickListener(v->showTransactionActions(transactionIndex));
            actions.setOnClickListener(v->showTransactionActions(transactionIndex));
            shown++;
        }
        if(shown==0)text(parent,"Tidak ada transaksi untuk filter ini",14,MUTED,false);
    }

    private void showTransactionActions(int index){
        JSONArray all=transactions();
        JSONObject transaction=all.optJSONObject(index);
        if(transaction==null)return;
        new AlertDialog.Builder(this).setTitle(transaction.optString("category","Transaksi"))
                .setItems(new String[]{"Ubah","Hapus"},(dialog,which)->{
                    if(which==0)editTransaction(index);
                    else new AlertDialog.Builder(this).setTitle("Hapus transaksi?")
                            .setMessage("Transaksi ini akan dihapus dari riwayat dan perhitungan anggaran.")
                            .setNegativeButton("Batal",null).setPositiveButton("Hapus",(confirm,selected)->{
                                JSONArray latest=transactions();
                                if(index<0||index>=latest.length()){toast("Transaksi tidak ditemukan");return;}
                                latest.remove(index);
                                prefs.edit().putString("transactions",latest.toString()).apply();
                                render();
                            }).show();
                }).show();
    }

    private void editTransaction(int index){
        JSONObject transaction=transactions().optJSONObject(index);
        if(transaction==null){toast("Transaksi tidak ditemukan");return;}
        editingTransactionIndex=index;
        draftAmount=String.valueOf(transaction.optLong("amount"));
        draftNote=transaction.optString("note","");
        selectedGroup=normalizeGroup(transaction.optString("group",GROUPS[0]));
        selectedCategory=transaction.optString("category",categoriesForGroup(selectedGroup)[0]);
        selectedCrumbs=transaction.optBoolean("crumbs",false);
        selectedWalletId=transaction.optString("walletId",DEFAULT_WALLET_ID);
        screen="Transaksi";
        render();
    }

    private void addTopCategoryRows(LinearLayout parent,long from){
        String[] categories=categoryNames();
        long[] totals=new long[categories.length];
        int[] order=new int[categories.length];
        for(int i=0;i<order.length;i++)order[i]=i;
        JSONArray all=transactions();
        for(int i=0;i<all.length();i++){
            JSONObject o=all.optJSONObject(i);
            if(o==null||o.optLong("date")<from)continue;
            for(int j=0;j<categories.length;j++)if(categories[j].equals(o.optString("category")))totals[j]+=o.optLong("amount");
        }
        for(int i=0;i<order.length;i++)for(int j=i+1;j<order.length;j++)if(totals[order[j]]>totals[order[i]]){int n=order[i];order[i]=order[j];order[j]=n;}
        int count=0;
        for(int idx:order){
            if(count>=3||totals[idx]==0)break;
            rowCard(parent,categories[idx],"Pengeluaran",money(totals[idx]),COLORS[count%COLORS.length]);
            count++;
        }
        if(count==0)text(parent,"Belum ada transaksi di periode ini.",14,MUTED,false);
    }
    private long groupSpent(String group){return groupSpentSince(group,periodStart());}
    private long groupSpentSince(String group,long from){long sum=0;JSONArray a=transactions();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&normalizeGroup(group).equals(normalizeGroup(o.optString("group")))&&o.optLong("date")>=from)sum+=o.optLong("amount");}return sum;}
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
        String[] categories=categoryNames();
        long[] totals=new long[categories.length];
        JSONArray all=transactions();
        for(int i=0;i<all.length();i++){
            JSONObject o=all.optJSONObject(i);
            if(o==null||!o.optBoolean("crumbs",false)||o.optLong("date")<from)continue;
            for(int j=0;j<categories.length;j++)if(categories[j].equals(o.optString("category")))totals[j]+=o.optLong("amount");
        }
        int[] order=new int[categories.length];
        for(int i=0;i<order.length;i++)order[i]=i;
        for(int i=0;i<order.length;i++)for(int j=i+1;j<order.length;j++)if(totals[order[j]]>totals[order[i]]){int n=order[i];order[i]=order[j];order[j]=n;}
        int count=0;
        for(int idx:order){
            if(count>=4||totals[idx]==0)break;
            rowCard(parent,categories[idx],"Jejak Recehan",money(totals[idx]),COLORS[count%COLORS.length]);
            count++;
        }
        if(count==0)text(parent,"Belum ada recehan tercatat minggu ini.",14,MUTED,false);
    }
    private int activeDaysThisWeek(){java.util.HashSet<String> days=new java.util.HashSet<>();JSONArray a=transactions();SimpleDateFormat f=new SimpleDateFormat("yyyyMMdd",Locale.ROOT);long start=weekStart();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&o.optLong("date")>=start)days.add(f.format(new Date(o.optLong("date"))));}return days.size();}
    private int daysToNextPayout(){Calendar today=Calendar.getInstance();today.set(Calendar.HOUR_OF_DAY,0);today.set(Calendar.MINUTE,0);today.set(Calendar.SECOND,0);today.set(Calendar.MILLISECOND,0);int payday=prefs.getInt("payoutDay",1);Calendar next=(Calendar)today.clone();next.set(Calendar.DAY_OF_MONTH,1);int effectiveDay=Math.min(payday,next.getActualMaximum(Calendar.DAY_OF_MONTH));if(today.get(Calendar.DAY_OF_MONTH)>=effectiveDay)next.add(Calendar.MONTH,1);next.set(Calendar.DAY_OF_MONTH,Math.min(payday,next.getActualMaximum(Calendar.DAY_OF_MONTH)));return Math.max(1,(int)((next.getTimeInMillis()-today.getTimeInMillis())/86400000L));}
    private void bottomNav(){LinearLayout nav=new LinearLayout(this);nav.setBackgroundColor(Color.WHITE);nav.setElevation(dp(8));page.addView(nav,new LinearLayout.LayoutParams(-1,dp(62)));String[] labels={"Beranda","Catat","Dompet","Recehan","Ringkasan"};String[] icons={"⌂","≡","▣","Ⓢ","⌁"};for(int i=0;i<labels.length;i++){String s=labels[i];TextView t=makeText(icons[i]+"\n"+s,10,screen.equals(s)?TEAL:MUTED,screen.equals(s));t.setGravity(Gravity.CENTER);nav.addView(t,new LinearLayout.LayoutParams(0,-1,1));t.setOnClickListener(v->{screen=s;render();});}}
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
