package de.ct.ctificate;

import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * ExpendableListAdapter for certificate chains supplied as JSON.
 */
public class CertificateChainAdapter extends BaseExpandableListAdapter {
    private JSONArray chain;
    private LayoutInflater inflater;

    public CertificateChainAdapter(LayoutInflater inflater, JSONArray chain) {
        this.inflater = inflater;
        this.chain = chain;
        Log.d("Adapter", "created");
    }

    @Override
    public int getGroupCount() {
        Log.d("Adapter group count", Integer.toString(this.chain.length()));
        return this.chain.length();
    }

    @Override
    public int getChildrenCount(int i) {
        Log.d("Adapter child count " + Integer.toString(i), Integer.toString(2));
        return 5;
    }

    @Override
    public Object getGroup(int i) {
        try {
            JSONObject singleCertificate = this.chain.getJSONObject(i);
            Log.d("Adapter get group (" + Integer.toString(i) + ")",
                    singleCertificate.getString("subject"));
            return singleCertificate.getString("subject");
        } catch (JSONException jsonError) {
            jsonError.printStackTrace();
            return "Certificate " + Integer.toString(i);
        }
    }

    @Override
    public Object getChild(int i, int i1) {
        try {
            JSONObject singleCertificate = this.chain.getJSONObject(i);
            switch (i1) {
                case 0: {
                    return singleCertificate.getString("description");
                }
                case 1: {
                    return singleCertificate.getString("pem");
                }
                case 2: {
                    return "MD5 fingerprint: " + singleCertificate.getString("md5");
                }
                case 3: {
                    return "SHA-1 fingerprint: " + singleCertificate.getString("sha1");
                }
                case 4: {
                    return "SHA-256 fingerprint: " + singleCertificate.getString("sha256");
                }
                default: {
                    return "Wrong child index: " + Integer.toString(i1);
                }
            }
        } catch (JSONException jsonError) {
            jsonError.printStackTrace();
            return "";
        }
    }

    @Override
    public long getGroupId(int i) {
        return i;
    }

    @Override
    public long getChildId(int i, int i1) {
        return i*10+i1;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int i, boolean isExpanded, View view, ViewGroup viewGroup) {
        Log.d("Adapter", "requesting group view");
        String headerTitle = (String) getGroup(i);
        if (view == null) {
            view = this.inflater.inflate(R.layout.certificate, null);
        }
        TextView certificateTitle = (TextView) view.findViewById(R.id.certificateTitle);
        certificateTitle.setTypeface(null, Typeface.BOLD);
        certificateTitle.setText(headerTitle);
        return view;
    }

    @Override
    public View getChildView(int i, int i1, boolean b, View view, ViewGroup viewGroup) {
        Log.d("Adapter", "requesting child view");
        final String childText = (String) getChild(i, i1);
        Log.d("Adapter", childText);
        if (view == null) {
            view = this.inflater.inflate(R.layout.certificate_item, null);
        }
        TextView txtListChild = (TextView) view.findViewById(R.id.certificateInfo);
        txtListChild.setText(childText);
        return view;
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return i1 == 1;
    }

    @Override
    public boolean areAllItemsEnabled()
    {
        return true;
    }

    @Override
    public void onGroupCollapsed(int groupPosition) {
        super.onGroupCollapsed(groupPosition);
    }

    @Override
    public void onGroupExpanded(int groupPosition) {
        super.onGroupExpanded(groupPosition);
    }
}
