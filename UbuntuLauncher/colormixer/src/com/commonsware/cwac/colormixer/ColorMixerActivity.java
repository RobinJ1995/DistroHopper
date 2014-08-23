/***
  Copyright (c) 2010-2013 CommonsWare, LLC
  
  Licensed under the Apache License, Version 2.0 (the "License"); you may
  not use this file except in compliance with the License. You may obtain
  a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package com.commonsware.cwac.colormixer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

public class ColorMixerActivity extends Activity {
  public static final String COLOR="c";
  public static final String TITLE="t";
  private ColorMixer mixer=null;
  
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
                         WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
    
    setContentView(R.layout.cwac_colormixer_activity);
    mixer=(ColorMixer)findViewById(R.id.mixer);
    
    String title=getIntent().getStringExtra(TITLE);
    
    if (title!=null) {
      setTitle(title);
    }
    
    mixer.setColor(getIntent().getIntExtra(COLOR, mixer.getColor()));
  }
  
  public void ok(View v) {
    Intent i=new Intent();
    
    i.putExtra(COLOR, mixer.getColor());
    
    setResult(RESULT_OK, i);
    finish();
  }
  
  @Override
  public void onSaveInstanceState(Bundle icicle) {
    super.onSaveInstanceState(icicle);
    
    icicle.putInt(COLOR, mixer.getColor());
  }

  @Override
  public void onRestoreInstanceState(Bundle icicle) {
    super.onRestoreInstanceState(icicle);
      
    mixer.setColor(icicle.getInt(COLOR));
  }
}