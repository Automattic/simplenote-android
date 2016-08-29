/**************************************************************************
 *
 * Copyright (C) 2012-2015 Alex Taradov <alex@taradov.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *************************************************************************/

package com.automattic.simplenote.utils;
 
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.automattic.simplenote.R;

public class Preferences extends PreferenceActivity
{
  @Override
  protected void onCreate(Bundle bundle)
  {
    super.onCreate(bundle);
    addPreferencesFromResource(R.xml.preferences);
  }
}

