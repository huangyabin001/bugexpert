/*
 * Copyright (C) 2013 Sony Mobile Communications AB
 *
 * This file is part of ChkBugReport.
 *
 * ChkBugReport is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * ChkBugReport is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ChkBugReport.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.bill.bugexpert.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SavedField {

    public enum Type {
        ID,
        LINK,
        INT,
        VARCHAR;

        public String sqldecl() {
            switch (this) {
            case ID: return "INTEGER PRIMARY KEY AUTOINCREMENT";
            case LINK:
            case INT: return "INT";
            case VARCHAR: return "VARCHAR";
            }
            return null;
        }
    }

    Type type();
}
