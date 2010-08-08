package com.android.todo.sync;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.android.todo.EditScreen;
import com.android.todo.TagToDoList;
import com.android.todo.data.ToDoDB;

public final class GlobalSearchHandler extends Activity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    startActivity(new Intent(this, EditScreen.class)
        .putExtra(EditScreen.EXTERNAL_INVOKER, true)
        .setAction(TagToDoList.ACTIVITY_EDIT_ENTRY + "")
        .putExtra(ToDoDB.KEY_NAME, getIntent().getData().getLastPathSegment())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    finish();
  }

}
