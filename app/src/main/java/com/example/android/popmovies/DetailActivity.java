package com.example.android.popmovies;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by jzhang on 4/29/16.
 */
public class DetailActivity extends ActionBarActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DetailFragment())
                    .commit();
        }
    }

    public static class DetailFragment extends Fragment {
        public DetailFragment() {
            setHasOptionsMenu(true);
        }

        private MovieDetails mDetails;
        private String posterPath;
        private String movieId;

        @Override
        public void onStart() {
            super.onStart();
            new FetchMovieDetailsTask().execute(movieId);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
            Intent intent = getActivity().getIntent();
            if (intent != null & intent.hasExtra(Intent.EXTRA_TEXT)) {
                String[] pathAndId = intent.getStringExtra(Intent.EXTRA_TEXT).split(" ");
                if (pathAndId.length == 2) {
                    this.posterPath = pathAndId[0];
                    this.movieId = pathAndId[1];
                }
            }
            return rootView;
        }

        private class MovieDetails {
            public MovieDetails() {}
            private String title;
            private String releaseDate;
            private Integer runTime;
            private double voteAverage;
            private String summary;

            public String getPosterPath() {
                return posterPath;
            }

            public void setPosterPath(String posterPath) {
                this.posterPath = posterPath;
            }

            private String posterPath;

            public String getTitle() {
                return title;
            }

            public void setTitle(String title) {
                this.title = title;
            }

            public String getReleaseDate() {
                return releaseDate;
            }

            public void setReleaseDate(String releaseDate) {
                this.releaseDate = releaseDate;
            }

            public Integer getRunTime() {
                return runTime;
            }

            public void setRunTime(Integer runTime) {
                this.runTime = runTime;
            }

            public double getVoteAverage() {
                return voteAverage;
            }

            public void setVoteAverage(double voteAverage) {
                this.voteAverage = voteAverage;
            }

            public String getSummary() {
                return summary;
            }

            public void setSummary(String summary) {
                this.summary = summary;
            }
        }
        public class FetchMovieDetailsTask extends AsyncTask<String, Void, MovieDetails> {
            private final String LOG_TAG = FetchMovieDetailsTask.class.getSimpleName();

            protected MovieDetails doInBackground(String... params) {
                // These two need to be declared outside the try/catch
                // so that they can be closed in the finally block.
                HttpURLConnection urlConnection = null;
                BufferedReader reader = null;

                // Will contain the raw JSON response as a string.
                String detailJsonStr;
                MovieDetails details = null;
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
                    detailJsonStr = buffer.toString();
                    details = getMovieDetailsFromJSon(detailJsonStr);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error ", e);
                    // If the code didn't successfully get the weather data, there's no point in attemping
                    // to parse it.
                    return null;
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "json", e);
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
                return details;
            }

            @Override
            protected void onPostExecute(MovieDetails details) {
                final String BASE_MOVIE_URL = "http://image.tmdb.org/t/p/w185/";
                if (details != null) {
                    FragmentActivity context = getActivity();
                    mDetails = details;

                    // Fill out views in details fragment layout.
                    ((TextView) context.findViewById(R.id.movie_title_textview))
                            .setText(mDetails.getTitle());
                    Picasso.with(context)
                            .load(BASE_MOVIE_URL + mDetails.getPosterPath())
                            .noFade().resize(600,600)
                            .centerCrop()
                            .into((ImageView) context.findViewById(R.id.img_movie_poster));
                    ((TextView) context.findViewById(R.id.tv_production_year))
                            .setText(mDetails.getReleaseDate());
                    ((TextView) context.findViewById(R.id.tv_duration))
                            .setText(String.format("%dm", mDetails.getRunTime()));
                    ((TextView) context.findViewById(R.id.tv_rating))
                            .setText(String.format("%.1f/10", mDetails.getVoteAverage()));
                }
            }

            private MovieDetails getMovieDetailsFromJSon(String detailJsonStr)
                    throws JSONException{
                final String TITLE = "title";
                final String RELEASE_DATE = "release_date";
                final String RUNTIME = "runtime";
                final String RATING = "vote_average";
                final String OVERVIEW = "overview";
                final String POSTER_PATH = "poster_path";
                JSONObject detailsJson = new JSONObject(detailJsonStr);
                MovieDetails details = new MovieDetails();
                details.setTitle(detailsJson.getString(TITLE));
                details.setReleaseDate(detailsJson.getString(RELEASE_DATE));
                details.setRunTime(detailsJson.getInt(RUNTIME));
                details.setSummary(detailsJson.getString(OVERVIEW));
                details.setVoteAverage(detailsJson.getDouble(RATING));
                details.setPosterPath(detailsJson.getString(POSTER_PATH));
                return details;
            }
        }
    }

}
