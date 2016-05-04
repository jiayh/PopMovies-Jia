package com.example.android.popmovies;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    public MainActivityFragment() {
    }

    private ImageAdapter mImageAdapter;
    private static final String BY_POPULARITY = "popular";
    private static final String By_RATING = "top_rated";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        inflater.inflate(R.menu.menu_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            updateMovies();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateMovies();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootview =  inflater.inflate(R.layout.fragment_main, container, false);
        GridView gridview = (GridView) rootview.findViewById(R.id.gridview);
        mImageAdapter = new ImageAdapter(getActivity(), new ArrayList<String>());
        gridview.setAdapter(mImageAdapter);

        gridview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                String pathAndId = parent.getItemAtPosition(position).toString();
                Intent intent = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, pathAndId);
                startActivity(intent);
            }
        });
        return rootview;
    }

    // Helper class and methods for retrieving movies from movieDB.
    public class ImageAdapter extends ArrayAdapter<String> {
        private LayoutInflater mInflater;
        private Context mContext;
        private ArrayList<String> pathAndIds;
        private final String BASE_MOVIE_URL = "http://image.tmdb.org/t/p/w185/";

        public ImageAdapter(Context c, ArrayList<String> items) {
            super(c, R.layout.movie_item_imageview, items);
            mContext = c;
            pathAndIds = items;
            mInflater = LayoutInflater.from(c);
        }

        @Override
        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView view = (ImageView) convertView;
            if (view == null) {
                view = new ImageView(getActivity());
            }

            String[] entries = pathAndIds.get(position).split(" ");

            Picasso.with(getActivity())
                    .load(BASE_MOVIE_URL + entries[0])
                    .noFade().resize(600,600)
                    .centerCrop()
                    .into(view);

            return view;
        }
    }

    private void updateMovies() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String sortBy = prefs.getString(getString(R.string.key_sortby),
                getString(R.string.sortby_popular));
        new FetchPopMoviesTask().execute(sortBy);
    }

    /**
     * Take the String representing the complete popular movies in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     * <p/>
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private String[] getMoviesDataFromJSon(String movieJSonStr)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        final String MOVIE_LIST = "results";
        final String POSTER_PATH = "poster_path";
        final String ID = "id";

        JSONObject moviesJSon = new JSONObject(movieJSonStr);
        JSONArray moviesArray = moviesJSon.getJSONArray(MOVIE_LIST);

        final int numMovies = moviesArray.length();

        // Each entry is of format "post_path id"
        String[] resultStrs = new String[numMovies];
        for (int i = 0; i < moviesArray.length(); i++) {
            // Get the JSON object representing the movie
            JSONObject movie = moviesArray.getJSONObject(i);
            resultStrs[i] = movie.getString(POSTER_PATH) + " "
            + movie.getInt(ID);
        }
        return resultStrs;
    }

    public class FetchPopMoviesTask extends AsyncTask<String, Void, String[]> {
        private final String LOG_TAG = FetchPopMoviesTask.class.getSimpleName();
        protected String[] doInBackground(String... params) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String moviesJsonStr;
            String[] movieResults = null;

            try {
                // Construct the URL for the MovieDB query
                // Possible parameters are avaiable at configuration page, at
                // http://docs.themoviedb.apiary.io/%23reference/configuration/configuration
                Uri uri = Uri.parse("https://api.themoviedb.org/3/movie")
                        .buildUpon()
                        .appendPath(params[0])
                        .appendQueryParameter("api_key", BuildConfig.MOVIEDB_API_KEY)
                        .build();
                URL url = new URL(uri.toString());
                Log.i("YINGHUAURL", url.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }

                // Parse JSon response.
                moviesJsonStr = buffer.toString();
                movieResults = getMoviesDataFromJSon(moviesJsonStr);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            return movieResults;
        }

        @Override
        protected void onPostExecute(String[] results) {
            if (results != null) {
                mImageAdapter.clear();
                for (String item : results) {
                    mImageAdapter.add(item);
                }
            }
        }
    }

}
