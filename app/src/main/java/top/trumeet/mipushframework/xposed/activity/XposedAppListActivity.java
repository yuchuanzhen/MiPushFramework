package top.trumeet.mipushframework.xposed.activity;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import top.trumeet.common.utils.Utils;
import top.trumeet.mipush.BuildConfig;
import top.trumeet.mipush.R;
import top.trumeet.mipushframework.MiPushFramework;
import top.trumeet.mipushframework.xposed.model.SimpleAppInfo;
import top.trumeet.mipushframework.xposed.util.CommonUtil;
import top.trumeet.mipushframework.xposed.util.SharedPreferencesHelper;

public class XposedAppListActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    // view component
    NormalRecyclerViewAdapter normalRecyclerViewAdapter = null;
    SwipeRefreshLayout swipeRefreshLayout = null;

    ArrayList<ApplicationInfo> displayAppInfo = new ArrayList<>();

    protected Activity getActivity() {
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xposed_app_list);


        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(normalRecyclerViewAdapter = new NormalRecyclerViewAdapter());

        swipeRefreshLayout = findViewById(R.id.SwipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this);

        XposedAppListActivity.this.onRefresh();

        if (!MiPushFramework.getInstance().isXposedWork()) {
            Toast.makeText(this, Utils.getString(R.string.xposed_not_working,
                    this), Toast.LENGTH_LONG).show();
        }

    }


    private Map<String, SimpleAppInfo> infoCache = new ConcurrentHashMap<>();

    @Override
    public void onRefresh() {
        LoadAppStatus loadAppStatus = new LoadAppStatus(this);
        loadAppStatus.execute();
    }

    private class NormalRecyclerViewAdapter extends RecyclerView.Adapter<NormalRecyclerViewAdapter.NormalTextViewHolder> implements Filterable {
        private ArrayList<ApplicationInfo> mOriginalValues = new ArrayList<>();

        @Override
        public NormalRecyclerViewAdapter.NormalTextViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new NormalTextViewHolder(LayoutInflater.from(XposedAppListActivity.this).inflate(R.layout.content_listview, parent, false));
        }

        @Override
        public void onBindViewHolder(final NormalRecyclerViewAdapter.NormalTextViewHolder holder, int position) {
            holder.applicationInfo = displayAppInfo.get(position);

            holder.tvTtitle.setText(holder.applicationInfo.loadLabel(getPackageManager()));
            holder.tvPkgName.setText(holder.applicationInfo.packageName);
            holder.imageView.setImageDrawable(holder.applicationInfo.loadIcon(getPackageManager()));

            SimpleAppInfo SimpleAppInfo = infoCache.get(holder.applicationInfo.packageName);
            holder.tvTtitle.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), (SimpleAppInfo != null && SimpleAppInfo.skipEnhance) ? R.color.red : R.color.bootstrap_brand_success));
        }

        @Override
        public int getItemCount() {
            return displayAppInfo.size();
        }

        public void setmOriginalValues(ArrayList<ApplicationInfo> mOriginalValues) {
            this.mOriginalValues = mOriginalValues;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();

                    if (constraint == null || constraint.length() == 0) {
                        ArrayList<ApplicationInfo> applicationInfoArrayList = new ArrayList<>(mOriginalValues);
                        filterResults.values = applicationInfoArrayList;
                        filterResults.count = applicationInfoArrayList.size();
                        return filterResults;
                    }

                    String prefixString = constraint.toString().toLowerCase();
                    final ArrayList<ApplicationInfo> values = mOriginalValues;
                    final int count = mOriginalValues.size();

                    final ArrayList<ApplicationInfo> newValues = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        final ApplicationInfo applicationInfo = values.get(i);
                        if (applicationInfo.packageName.toLowerCase().contains(prefixString) || applicationInfo.loadLabel(getPackageManager()).toString().toLowerCase().contains(prefixString))
                            newValues.add(applicationInfo);
                    }

                    filterResults.values = newValues;
                    filterResults.count = newValues.size();

                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    displayAppInfo = (ArrayList<ApplicationInfo>) results.values;
                    notifyDataSetChanged();
                }
            };
        }

        class NormalTextViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            private TextView tvTtitle;
            private TextView tvPkgName;
            private ImageView imageView;
            private ApplicationInfo applicationInfo;

            NormalTextViewHolder(View itemView) {
                super(itemView);
                tvTtitle = itemView.findViewById(R.id.text1);
                tvPkgName = itemView.findViewById(R.id.text2);
                imageView = itemView.findViewById(R.id.image_view);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                SimpleAppInfo SimpleAppInfo = SharedPreferencesHelper.getInstance().get(tvPkgName.getText().toString(), "", top.trumeet.mipushframework.xposed.model.SimpleAppInfo.class);
                if (SimpleAppInfo == null) {
                    SimpleAppInfo = new SimpleAppInfo();
                    SimpleAppInfo.skipEnhance = true;
                } else {
                    SimpleAppInfo.skipEnhance = !SimpleAppInfo.skipEnhance;
                }

                SharedPreferencesHelper.getInstance().update(tvPkgName.getText().toString(), SimpleAppInfo);
                tvTtitle.setTextColor(ContextCompat.getColor(v.getContext(), (SimpleAppInfo.skipEnhance) ? R.color.red : R.color.bootstrap_brand_success));

                infoCache.put(applicationInfo.packageName, SimpleAppInfo);

                Toast.makeText(getActivity(), Utils.getString(R.string.restart_required, getActivity()), Toast.LENGTH_LONG).show();

            }
        }
    }


    private static class LoadAppStatus extends AsyncTask<Object, Void, Void> {
        private WeakReference<XposedAppListActivity> activityReference;

        LoadAppStatus(XposedAppListActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            activityReference.get().swipeRefreshLayout.setRefreshing(true);
            activityReference.get().normalRecyclerViewAdapter.notifyItemRangeRemoved(0, activityReference.get().displayAppInfo.size());
            activityReference.get().displayAppInfo.clear();
        }

        @Override
        protected Void doInBackground(Object... params) {
            Set<String> ignorePackageName = new HashSet<>();
            ignorePackageName.add("android");
            ignorePackageName.add(BuildConfig.APPLICATION_ID);
            ignorePackageName.add("de.robv.android.xposed.installer");

            int hasConfigIndex = 0;

            List<ApplicationInfo> applicationInfoList = activityReference.get().getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
            Collections.sort(applicationInfoList, new ApplicationInfo.DisplayNameComparator(activityReference.get().getPackageManager()));

            for (ApplicationInfo applicationInfo : applicationInfoList) {
                if (ignorePackageName.contains(applicationInfo.packageName)) {
                    continue;
                }
                if (CommonUtil.isUserApplication(applicationInfo)) {
                    SimpleAppInfo SimpleAppInfo = SharedPreferencesHelper.getInstance().get(applicationInfo.packageName, "", top.trumeet.mipushframework.xposed.model.SimpleAppInfo.class);
                    if (SimpleAppInfo != null) {
                        activityReference.get().infoCache.put(applicationInfo.packageName, SimpleAppInfo);
                        activityReference.get().displayAppInfo.add(hasConfigIndex++, applicationInfo);
                    } else {
                        activityReference.get().displayAppInfo.add(applicationInfo);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void o) {
            activityReference.get().normalRecyclerViewAdapter.setmOriginalValues(activityReference.get().displayAppInfo);
            activityReference.get().normalRecyclerViewAdapter.notifyDataSetChanged();
            activityReference.get().swipeRefreshLayout.setRefreshing(false);
        }

    }

}
