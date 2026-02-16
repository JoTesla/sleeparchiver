/*
 * SleepArchiver - cross-platform data manager for Sleeptracker-series watches.
 * Copyright (C) 2009-2011 Pavel Fatin <http://pavelfatin.com>
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
 */

package com.pavelfatin.sleeparchiver.model;

import jakarta.xml.bind.*;
import jakarta.xml.bind.annotation.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@XmlRootElement(name = "preferences", namespace = "")
@XmlAccessorType(value = XmlAccessType.FIELD)
public class Preferences {
    @XmlTransient
    private File _file;

    @XmlElement(name = "language", namespace = "")
    private Language _language;

    @XmlElement(name = "backups", namespace = "")
    private boolean _backups;

    @XmlElement(name = "prefill", namespace = "")
    private boolean _prefill;

    @XmlElement(name = "history", namespace = "")
    private boolean _history;

    @XmlElement(name = "historyLimit", namespace = "")
    private int _historyLimit;

    @XmlElement(name = "openRecent", namespace = "")
    private boolean _openRecent;

    @XmlElement(name = "file", namespace = "")
    @XmlElementWrapper(name = "files", namespace = "")
    private List<String> _files = new ArrayList<>();

    // Grid settings
    @XmlElement(name = "manualGrid", namespace = "")
    private boolean _manualGrid;

    @XmlElement(name = "gridStartHour", namespace = "")
    private int _gridStartHour = 22;

    @XmlElement(name = "gridEndHour", namespace = "")
    private int _gridEndHour = 10;

    // Display settings
    @XmlElement(name = "displayMode", namespace = "")
    private String _displayMode = "month";

    @XmlElement(name = "displayDays", namespace = "")
    private int _displayDays = 30;

    @XmlElement(name = "displayMonth", namespace = "")
    private String _displayMonth;

    // Sort order
    @XmlElement(name = "sortOrder", namespace = "")
    private String _sortOrder = "desc";

    // Debug logging
    @XmlElement(name = "debugLogging", namespace = "")
    private boolean _debugLogging;

    // Last port/model
    @XmlElement(name = "lastPort", namespace = "")
    private String _lastPort;

    @XmlElement(name = "lastModel", namespace = "")
    private String _lastModel;


    public Preferences() {
    }

    public void setFile(File file) {
        _file = file;
    }

    public Language getLanguage() {
        return _language;
    }

    public void setLanguage(Language language) {
        _language = language;
    }

    public boolean isBackupsEnabled() {
        return _backups;
    }

    public void setBackupsEnabled(boolean enabled) {
        _backups = enabled;
    }

    public boolean isPrefillEnabled() {
        return _prefill;
    }

    public void setPrefillEnabled(boolean enabled) {
        _prefill = enabled;
    }

    public boolean isHistoryEnabled() {
        return _history;
    }

    public void setHistoryEnabled(boolean enabled) {
        _history = enabled;
        truncateRecentFilesList();
    }

    public int getHistoryLimit() {
        return _historyLimit;
    }

    public void setHistoryLimit(int limit) {
        _historyLimit = limit;
        truncateRecentFilesList();
    }

    public boolean isOpenRecentEnabled() {
        return _openRecent;
    }

    public void setOpenRecentEnabled(boolean enabled) {
        _openRecent = enabled;
    }

    public String getSortOrder() {
        return _sortOrder != null ? _sortOrder : "desc";
    }

    public void setSortOrder(String order) {
        _sortOrder = order;
    }

    public boolean isDebugLogging() {
        return _debugLogging;
    }

    public void setDebugLogging(boolean enabled) {
        _debugLogging = enabled;
    }

    public boolean isManualGrid() {
        return _manualGrid;
    }

    public void setManualGrid(boolean manual) {
        _manualGrid = manual;
    }

    public int getGridStartHour() {
        return _gridStartHour;
    }

    public void setGridStartHour(int hour) {
        _gridStartHour = hour;
    }

    public int getGridEndHour() {
        return _gridEndHour;
    }

    public void setGridEndHour(int hour) {
        _gridEndHour = hour;
    }

    public String getDisplayMode() {
        return _displayMode;
    }

    public void setDisplayMode(String mode) {
        _displayMode = mode;
    }

    public int getDisplayDays() {
        return _displayDays;
    }

    public void setDisplayDays(int days) {
        _displayDays = days;
    }

    public String getDisplayMonth() {
        return _displayMonth;
    }

    public void setDisplayMonth(String month) {
        _displayMonth = month;
    }

    public String getLastPort() {
        return _lastPort;
    }

    public void setLastPort(String port) {
        _lastPort = port;
    }

    public String getLastModel() {
        return _lastModel;
    }

    public void setLastModel(String model) {
        _lastModel = model;
    }

    public String getRecentFile() {
        if (!hasRecentFiles()) {
            throw new RuntimeException("Recent files list is empty");
        }
        return _files.get(0);
    }

    public File getRecentDirectory() {
        return new File(getRecentFile()).getParentFile();
    }

    public static Preferences createDefault(File file, Language language) {
        Preferences preferences = new Preferences();
        preferences.setFile(file);
        preferences.setLanguage(language);
        preferences.setBackupsEnabled(true);
        preferences.setPrefillEnabled(true);
        preferences.setHistoryEnabled(true);
        preferences.setHistoryLimit(5);
        preferences.setOpenRecentEnabled(true);
        return preferences;
    }

    public List<String> getRecentFiles() {
        return Collections.unmodifiableList(_files);
    }

    public boolean hasRecentFiles() {
        return !_files.isEmpty();
    }

    public void addRecentFile(String file) {
        _files.remove(file);
        _files.add(0, file);
        truncateRecentFilesList();
    }

    public void clearRecentFiles() {
        _files.clear();
    }

    private void truncateRecentFilesList() {
        int count = _history ? _historyLimit : 0;
        if (_files.size() > count) {
            _files = new ArrayList<>(_files.subList(0, count));
        }
    }

    public static Preferences loadOrCreateDefault(File file, Language language) {
        try {
            return load(file);
        } catch (IOException e) {
            return createDefault(file, language);
        }
    }

    public static Preferences load(File file) throws IOException {
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            Preferences preferences = loadFrom(in);
            preferences.setFile(file);
            return preferences;
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    static Preferences loadFrom(InputStream stream) throws JAXBException {
        Unmarshaller unmarshaller = createContext().createUnmarshaller();
        unmarshaller.setEventHandler(new ValidationHandler());
        return (Preferences) unmarshaller.unmarshal(stream);
    }

    private static JAXBContext createContext() throws JAXBException {
        return JAXBContext.newInstance(Preferences.class);
    }

    public void save() throws IOException {
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(_file))) {
            saveTo(out);
            out.flush();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    void saveTo(OutputStream stream) throws JAXBException {
        Marshaller marshaller = createContext().createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setEventHandler(new ValidationHandler());
        marshaller.marshal(this, stream);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Preferences other = (Preferences) obj;
        return _backups == other._backups
                && _prefill == other._prefill
                && _history == other._history
                && _historyLimit == other._historyLimit
                && _openRecent == other._openRecent
                && _debugLogging == other._debugLogging
                && _manualGrid == other._manualGrid
                && _gridStartHour == other._gridStartHour
                && _gridEndHour == other._gridEndHour
                && _displayDays == other._displayDays
                && Objects.equals(_file, other._file)
                && Objects.equals(_language, other._language)
                && Objects.equals(_files, other._files)
                && Objects.equals(_displayMode, other._displayMode)
                && Objects.equals(_displayMonth, other._displayMonth)
                && Objects.equals(_lastPort, other._lastPort)
                && Objects.equals(_lastModel, other._lastModel)
                && Objects.equals(_sortOrder, other._sortOrder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_file, _language, _backups, _prefill,
                _history, _historyLimit, _openRecent, _files,
                _debugLogging, _manualGrid, _gridStartHour, _gridEndHour,
                _displayMode, _displayDays, _displayMonth,
                _lastPort, _lastModel, _sortOrder);
    }

    @Override
    public String toString() {
        return Objects.toString(_file) + ", " + _language + ", " +
                _backups + ", " + _prefill + ", " +
                _history + ", " + _historyLimit + ", " +
                _openRecent + ", " + _files;
    }
}
