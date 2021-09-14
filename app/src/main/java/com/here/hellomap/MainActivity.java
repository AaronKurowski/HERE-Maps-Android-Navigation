/*
 * Copyright (C) 2019-2021 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package com.here.hellomap;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.here.hellomap.PermissionsRequestor.ResultListener;
import com.here.sdk.core.Anchor2D;
import com.here.sdk.core.Color;
import com.here.sdk.core.GeoBox;
import com.here.sdk.core.GeoCoordinates;
import com.here.sdk.core.GeoPolyline;
import com.here.sdk.core.LanguageCode;
import com.here.sdk.core.Metadata;
import com.here.sdk.core.Point2D;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.gestures.TapListener;
import com.here.sdk.mapview.MapCamera;
import com.here.sdk.mapview.MapError;
import com.here.sdk.mapview.MapImage;
import com.here.sdk.mapview.MapImageFactory;
import com.here.sdk.mapview.MapMarker;
import com.here.sdk.mapview.MapPolyline;
import com.here.sdk.mapview.MapScene;
import com.here.sdk.mapview.MapScheme;
import com.here.sdk.mapview.MapView;
import com.here.sdk.mapview.MapViewBase;
import com.here.sdk.mapview.PickMapItemsResult;
import com.here.sdk.search.AddressQuery;
import com.here.sdk.search.Place;
import com.here.sdk.search.PlaceCategory;
import com.here.sdk.search.SearchCallback;
import com.here.sdk.search.SearchEngine;
import com.here.sdk.search.SearchError;
import com.here.sdk.search.SearchOptions;
import com.here.sdk.search.SuggestCallback;
import com.here.sdk.search.Suggestion;
import com.here.sdk.search.TextQuery;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private PermissionsRequestor permissionsRequestor;
    private MapView mapView;

    private int styleCounter = 0;
    private MapScheme scheme = MapScheme.NORMAL_DAY;

    private int cameraCounter = 0;
    private GeoCoordinates cameraCoordinates;
    private double bearingInDegrees;
    private double tiltInDegrees;
    private MapCamera.OrientationUpdate cameraOrientation;
    private double distanceInMeters;

    private SearchEngine searchEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get a MapView instance from the layout.
        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);

        mapView.setOnReadyListener(new MapView.OnReadyListener() {
            @Override
            public void onMapViewReady() {
                // This will be called each time after this activity is resumed.
                // It will not be called before the first map scene was loaded.
                // Any code that requires map data may not work as expected beforehand.
                Log.d(TAG, "HERE Rendering Engine attached.");
            }
        });

        handleAndroidPermissions();

        try {
            searchEngine = new SearchEngine();

        } catch (InstantiationErrorException e) {
           throw new RuntimeException("Something went wrong: " + e);
        }
    }

    private void handleAndroidPermissions() {
        permissionsRequestor = new PermissionsRequestor(this);
        permissionsRequestor.request(new ResultListener(){

            @Override
            public void permissionsGranted() {
                loadMapScene();
            }

            @Override
            public void permissionsDenied() {
                Log.e(TAG, "Permissions denied by user.");
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsRequestor.onRequestPermissionsResult(requestCode, grantResults);
    }

    private void loadMapScene() {
        // Load a scene from the HERE SDK to render the map with a map scheme.
        mapView.getMapScene().loadScene(MapScheme.NORMAL_DAY, new MapScene.LoadSceneCallback() {
            @Override
            public void onLoadScene(@Nullable MapError mapError) {
                if (mapError == null) {
                    double distanceInMeters = 1000 * 10;
                    mapView.getCamera().lookAt(
                            new GeoCoordinates(40.7831, -73.9712), distanceInMeters);
                } else {
                    Log.d(TAG, "Loading map failed: mapError: " + mapError.name());
                }
            }
        });
    }

    public void changeMapStyle(View view) {
        styleCounter++;
        if(styleCounter == 4) styleCounter = 0;

        if(styleCounter == 0) scheme = MapScheme.NORMAL_DAY;
        if(styleCounter == 1) scheme = MapScheme.SATELLITE;
        if(styleCounter == 2) scheme = MapScheme.NORMAL_NIGHT;
        if(styleCounter == 3) scheme = MapScheme.HYBRID_DAY;

        mapView.getMapScene().loadScene(scheme, new MapScene.LoadSceneCallback() {
            @Override
            public void onLoadScene(MapError mapError) {
                if(mapError == null) {
                    // do something
                    // TODO: Write onLoadScene method
                } else {
                    // error handling
                }
            }
        });
    }

    public void loadStyle(View view) {
        // Method to add a custom map style. Use style editor offered by HERE
        String filename = "omv-traffic-traffic-normal-night.scene.json";
        AssetManager assetManager = this.getAssets();

        try {
            assetManager.open(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mapView.getMapScene().loadScene("" + filename, new MapScene.LoadSceneCallback() {
            @Override
            public void onLoadScene(MapError mapError) {
                if(mapError == null) {
                    // do something
                } else {
                    // error handling
                }
            }
        });
    }

    public void changeCamera(View view) {
        // To fix deprecation use:
        // GeoCoordinationUpdate cameraOrientation = new GeoCoordinateUpdate(bearingInDegrees, tiltInDegrees);

        cameraCounter++;
        if(cameraCounter == 4) cameraCounter = 0;

        cameraCoordinates = new GeoCoordinates(40.7831, -73.9712);
        bearingInDegrees = 0;
        tiltInDegrees = 0;
        cameraOrientation = new MapCamera.OrientationUpdate(bearingInDegrees, tiltInDegrees);
        distanceInMeters = 1000 * 10;

        if(cameraCounter == 0) {
            mapView.getCamera().lookAt(cameraCoordinates, cameraOrientation, distanceInMeters);

        } else if(cameraCounter == 1) {

            bearingInDegrees = 90;
            cameraOrientation = new MapCamera.OrientationUpdate(bearingInDegrees, tiltInDegrees);
            mapView.getCamera().lookAt(cameraCoordinates, cameraOrientation, distanceInMeters);

        } else if(cameraCounter == 2) {

            tiltInDegrees = 45;
            distanceInMeters = 1000 * 2;
            cameraOrientation = new MapCamera.OrientationUpdate(bearingInDegrees, tiltInDegrees);
            mapView.getCamera().lookAt(cameraCoordinates, cameraOrientation, distanceInMeters);

        } else if(cameraCounter == 3) {

            // create a rectangle with two sets of coordinates
            GeoBox cameraBox = new GeoBox(new GeoCoordinates(40.72537, -73.98401), new GeoCoordinates(40.72757, -73.9793));

            mapView.getCamera().lookAt(cameraBox, cameraOrientation);

        }
    }

    public void addMarker(View view) {
        // Create MapImage
        MapImage mapImage = MapImageFactory.fromResource(this.getResources(), R.drawable.marker);

        // Create Anchor
        Anchor2D anchor2D = new Anchor2D(0.5f, 1.0f);

        // Add metadata
        Metadata metadata = new Metadata();
        metadata.setString("key_poi", "This is New York");

        // Create MapMarker
        MapMarker mapMarker = new MapMarker(new GeoCoordinates(40.70055, -74.0086), mapImage, anchor2D);
        mapMarker.setMetadata(metadata);

        // Add the marker to the map
        mapView.getMapScene().addMapMarker(mapMarker);
        setTapGestureHandler();
    }

    public void addPolyline(View view) {
        // Create a GeoPolyline
        ArrayList<GeoCoordinates> polylineCoordinates = new ArrayList();
        polylineCoordinates.add(new GeoCoordinates(40.70638, -74.01896));
        polylineCoordinates.add(new GeoCoordinates(40.70127, -74.01497));
        polylineCoordinates.add(new GeoCoordinates(40.70329, -74.00746));
        polylineCoordinates.add(new GeoCoordinates(40.70797, -73.99961));

        GeoPolyline geoPolyline;
        try {
            geoPolyline = new GeoPolyline(polylineCoordinates);
        } catch(InstantiationErrorException e) {
            // Only for now. Right error handling later
            geoPolyline = null;
        }

        // Define the style of the polyline
        float widthInPixels = 20;
        Color lineColor = Color.valueOf(0, 0.56f, 0.54f, 0.63f);

        // Create MapPolyline
        MapPolyline mapPolyline = new MapPolyline(geoPolyline, widthInPixels, lineColor);

        // Add that to the map
        mapView.getMapScene().addMapPolyline(mapPolyline);
    }

    public void setTapGestureHandler() {
        mapView.getGestures().setTapListener(new TapListener() {
            @Override
            public void onTap(Point2D touchPoint) {
                mapView.pickMapItems(touchPoint, 2, new MapViewBase.PickMapItemsCallback() {
                    @Override
                    public void onPickMapItems(PickMapItemsResult pickMapItemsResult) {
                        List<MapMarker> mapMarkerList = pickMapItemsResult.getMarkers();
                        MapMarker pickedMarker = mapMarkerList.get(0);
                        Metadata metadata = pickedMarker.getMetadata();

                        if(metadata != null) {
                            String message = "No message found.";
                            String string = metadata.getString("key_poi");
                            if(string != null) {
                                message = string;
                            }
                            Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
                            toast.show();
                        }
//                        if(metadata == null) {
//                            Toast toast = Toast.makeText(getApplicationContext(), "Not here", Toast.LENGTH_SHORT);
//                            toast.show();
//                        } else if(metadata.getString("New York").equals("New York")) {
//                            Toast toast = Toast.makeText(getApplicationContext(), "You found New York", Toast.LENGTH_SHORT);
//                            toast.show();
//                        }
                    }
                });
            }
        });
    }

    public void searchPlaces(View view) {
        // Searches for places around center of screen
        int maxItems = 5;
        SearchOptions searchOptions = new SearchOptions(LanguageCode.EN_US, maxItems);
        EditText editText = findViewById(R.id.searchText);
        TextQuery textQuery = new TextQuery(editText.getText().toString(), getScreenCenter());

        searchEngine.search(textQuery, searchOptions, new SearchCallback() {
            @Override
            public void onSearchCompleted(SearchError searchError, List<Place> list) {
                for(Place result : list) {
                    TextView textView = new TextView(getApplicationContext());
                    textView.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                    textView.setText(result.getTitle() + "\n" + result.getAddress().addressText);

                    LinearLayout linearLayout = new LinearLayout(getApplicationContext());
                    linearLayout.setBackgroundResource(R.color.colorPrimary);
                    linearLayout.setPadding(10, 10, 10, 10);
                    linearLayout.addView(textView);

                    mapView.pinView(linearLayout, result.getGeoCoordinates());
                }
            }
        });
    }

    public void searchAddress(View view) {
        // Search by specific text input
        int maxItems = 1;
        SearchOptions searchOptions = new SearchOptions(LanguageCode.EN_US, maxItems);
        EditText editText = findViewById(R.id.searchText);
        AddressQuery addressQuery = new AddressQuery(editText.getText().toString(), getScreenCenter());

        searchEngine.search(addressQuery, searchOptions, new SearchCallback() {
            @Override
            public void onSearchCompleted(SearchError searchError, List<Place> list) {
                for(Place result : list) {
                    TextView textView = new TextView(getApplicationContext());
                    textView.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                    textView.setText(result.getTitle() + "\n" + result.getAddress().addressText);

                    LinearLayout linearLayout = new LinearLayout(getApplicationContext());
                    linearLayout.setBackgroundResource(R.color.colorPrimary);
                    linearLayout.setPadding(10, 10, 10, 10);
                    linearLayout.addView(textView);

                    mapView.pinView(linearLayout, result.getGeoCoordinates());

                    mapView.getCamera().lookAt(result.getGeoCoordinates());
                }
            }
        });

    }

    public void searchAutoSuggest(View view) {

        int maxItems = 3;
        SearchOptions searchOptions = new SearchOptions(LanguageCode.EN_US, maxItems);
        EditText editText = findViewById(R.id.searchText);
        TextQuery textQuery = new TextQuery(editText.getText().toString(), getScreenCenter());

        searchEngine.suggest(textQuery, searchOptions, new SuggestCallback() {
            @Override
            public void onSuggestCompleted(SearchError searchError, List<Suggestion> list) {
                // Error
                ArrayList<String> arrayList = new ArrayList<>();

                for(Suggestion suggestResult : list) {
                    Place place = suggestResult.getPlace();
                    if(place != null) {
                        arrayList.add(place.getTitle());
                    } else {
                        arrayList.add(suggestResult.getTitle());
                    }
                }

                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, arrayList);

                ListView listView = findViewById(R.id.suggestionsListView);
                listView.setAdapter(arrayAdapter);

            }
        });
    }

    private GeoCoordinates getScreenCenter() {
        int screenWidthInPixels = mapView.getWidth();
        int screenHeightInPixels = mapView.getHeight();
        Point2D point = new Point2D(screenWidthInPixels * 0.5, screenHeightInPixels * 0.5);
        return mapView.viewToGeoCoordinates(point);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
}
