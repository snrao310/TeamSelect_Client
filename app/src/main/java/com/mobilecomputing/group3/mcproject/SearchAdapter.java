package com.mobilecomputing.group3.mcproject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sureshgururajan on 4/19/16.
 */
public class SearchAdapter extends ArrayAdapter{
    private List list= new ArrayList();
    private final int MAX_ENTRIES = 5;

    JSONArray userList;
    String userName2,userName1;
    String ip=new IP().getIP();
    Button globalButton;

    public SearchAdapter(Context context, int resource,String userName1) {
        super(context, resource);
        this.userList=userList;
        this.userName1=userName1;
    }

    public void add(SearchClass object) {
        list.add(object);
        super.add(object);
    }

    static class SearchHolder
    {
        TextView NAME;
        Button meet;
        Button add;
    }

    public void clearAll() {
        super.clear();
        list.clear();
    }

    @Override
    public int getCount() {
//        return Math.min(MAX_ENTRIES, this.list.size());
        return this.list.size();
    }

    @Override
    public Object getItem(int position) {
        return this.list.get(position);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View row;
        row = convertView;
        final SearchHolder holder;

        if(convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.userlist,parent,false);
            holder = new SearchHolder();
            holder.NAME = (TextView) row.findViewById(R.id.txtUsername);
            row.setTag(holder);
        }
        else
        {
            holder = (SearchHolder) row.getTag();
        }

        SearchClass FR = (SearchClass) getItem(position);
        holder.NAME.setText(FR.getName());


        holder.add=(Button) row.findViewById(R.id.reqButton);
        holder.add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    globalButton=holder.add;
                    //Toast.makeText(getContext(), userList.getJSONObject(position).get("username").toString(), Toast.LENGTH_LONG).show();
                    SearchClass FR=(SearchClass)getItem(position);
                    userName2=FR.getUsername();
                    SendInfo s=new SendInfo();
                    s.execute();
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        });

        holder.meet = (Button) row.findViewById(R.id.meetButton);
        holder.meet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // 1. Get source username, target username, source user's current location
                    SearchClass FR=(SearchClass)getItem(position);
                    userName2=FR.getUsername();
                    double[] location = (new LocList()).getLocation();

                    // 2. These are the details you need to send to server
                    String sourceUsername = userName1;
                    String targetUsername = userName2;
                    double latitude_cur = location[0];
                    double longitude_cur = location[1];
                    String[] details = new String[4];

                    // 3. Pack the above four details into a string array
                    details[0] = sourceUsername;
                    details[1] = targetUsername;
                    details[2] = String.valueOf(latitude_cur);
                    details[3] = String.valueOf(longitude_cur);

                    // 4. And send it to the server
                    GetMeetRequestsTask makeMeetRequest = new GetMeetRequestsTask();
                    makeMeetRequest.execute(details);

                    // 5. And hope the receiver accepts the message
                } catch (Exception ex) {
                    Toast.makeText(getContext(),ex.getMessage(),Toast.LENGTH_LONG).show();
                }
            }

            class LocList implements LocationListener {
                private LocationManager locMgr;
                private double latitude, longitude;
                private Location location;

                public LocList() {
                    try {
                        locMgr = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
                        locMgr.requestLocationUpdates( LocationManager.GPS_PROVIDER, 2000, 10, this);
                        Location location_g = locMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        Location location_n = locMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if ( location_g != null ) {
                            this.location = location_g;
                        }
                        if ( location_g == null && location_n != null ) {
                            this.location = location_n;
                        }
                    } catch (SecurityException ex) {
                        Log.i("EXCEPTION:", "Permission denied");
                    }
                }

                public LocationManager getLocationManager() {
                    return locMgr;
                }

                @Override
                public void onLocationChanged(Location location) {
                    this.latitude = location.getLatitude();
                    this.longitude = location.getLongitude();
                    this.location = location;
                }

                @Override
                public void onProviderDisabled(String provider) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    getContext().startActivity(intent);
                    Toast.makeText(getContext(), "Gps is turned off!! ",
                            Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onProviderEnabled(String provider) {
                    Toast.makeText(getContext(), "Gps is turned on!! ",
                            Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                public double getLatitude() {
                    return this.latitude;
                }

                public double getLongitude() {
                    return this.longitude;
                }

                public double[] getLocation() {
                    double[] loc_details = new double[2];
                    loc_details[0] = location.getLatitude();
                    loc_details[1] = location.getLongitude();
                    return loc_details;
                }
            }

            class GetMeetRequestsTask extends AsyncTask<String, Void, String> {
                public String meetRequester = "";

                @Override
                protected String doInBackground(String ... details) {
                    try {
                        URL url = new URL("http://" + new IP().getIP() + ":3000/makemeetrequest");

                        String data = URLEncoder.encode("fromUsername", "UTF-8") + "=" + URLEncoder.encode(details[0], "UTF-8") + "&";
                        data+= URLEncoder.encode("toUsername", "UTF-8") + "=" + URLEncoder.encode(details[1], "UTF-8") + "&";
                        data+= URLEncoder.encode("latitude", "UTF-8") + "=" + URLEncoder.encode(details[2], "UTF-8") + "&";
                        data+= URLEncoder.encode("longitude", "UTF-8") + "=" + URLEncoder.encode(details[3], "UTF-8");

                        HttpURLConnection conn = (HttpURLConnection)url.openConnection();

                        conn.setRequestMethod("POST");
                        conn.setDoOutput(true);
                        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());

                        wr.write(data);
                        wr.flush();

                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line = reader.readLine();

                        meetRequester = line;

                        return line;
                    } catch(Exception ex) {
                        return null; // Error processing request/response
                    }
                }

                @Override
                protected void onPostExecute(String s) {  }
            }

        });
        return row;
    }




















    class SendInfo extends AsyncTask<String, Void, String> {

        String text="";

        public void sendReqInfo() throws UnsupportedEncodingException {
            // Create data variable for sent values to server
            String data = URLEncoder.encode("fromUsername", "UTF-8")
                    + "=" + URLEncoder.encode(userName1, "UTF-8");

            data += "&" + URLEncoder.encode("toUsername", "UTF-8") + "="
                    + URLEncoder.encode(userName2, "UTF-8");

            BufferedReader reader = null;

            // Send data
            try {
                // Defined URL  where to send data
                URL url = new URL("http://"+ip+":3000/request");

                // Send POST data request
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
                //conn.connect();
                conn.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(data);
                wr.flush();

                // Get the server response
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line = null;

                // Read Server Response
                while ((line = reader.readLine()) != null) {
                    // Append server response in string
                    sb.append(line);
                }
                text = sb.toString();
            } catch (Exception ex) {}
            finally {
                try {
                    reader.close();
                } catch (Exception ex) {
                }
            }
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                // CALL GetText method to make post method call
                sendReqInfo();
            } catch (Exception ex) {}
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            if(text.equals("0")){
                Toast.makeText(getContext(),"Error Request couldn't be sent",Toast.LENGTH_LONG).show();
            }
            else {
                Log.i("ABC", text);
                Toast.makeText(getContext(), "Request Sent", Toast.LENGTH_LONG).show();
                globalButton.setText("Request Sent");
                globalButton.setClickable(false);
                globalButton.setBackgroundColor(Color.LTGRAY);
            }
        }
    }
}
