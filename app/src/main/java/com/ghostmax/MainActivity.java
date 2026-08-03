package com.ghostmax;

import android.app.AlertDialog;
import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.speech.RecognizerIntent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.os.VibrationEffect;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.*;

public class MainActivity extends AppCompatActivity {
    private Prefs prefs;
    private EditText inputText;
    private ImageButton sendButton, stopButton, copyButton, micButton, pasteButton, learningBtn;
    private ActivityResultLauncher<Intent> speechLauncher;
    private ActivityResultLauncher<String> micPermissionLauncher;
    private Spinner providerSpinner, modeSpinner, personalitySpinner;
    private RecyclerView chatRecycler;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> history = new ArrayList<>();
    private AtomicBoolean isProcessing = new AtomicBoolean(false);
    private Thread currentThread;
    private OkHttpClient httpClient = new OkHttpClient();
    private List<PersonalityManager.Personality> allPers;
    private String currentPersonality = "Freundlicher Assistent";
    private String currentMode = "Smart";
    private boolean secretMode = false;
    private LearningEngine learningEngine;

    private final Handler inactivityHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoSecretRunnable = () -> {
        if (!secretMode && prefs.getAutoSecretMinutes() > 0) {
            activateSecretMode();
            Toast.makeText(MainActivity.this, "🕵️ Geheim-Modus automatisch aktiviert", Toast.LENGTH_LONG).show();
        }
    };
    private final Handler loadingAnimHandler = new Handler(Looper.getMainLooper());
    private int loadingDots = 0;
    private final Runnable loadingAnimRunnable = new Runnable() {
        @Override public void run() {
            loadingDots = (loadingDots + 1) % 4;
            StringBuilder sb = new StringBuilder("⏳ Antwort wird geladen");
            for (int i=0; i<loadingDots; i++) sb.append(".");
            chatAdapter.updateLastText(sb.toString());
            loadingAnimHandler.postDelayed(this, 400);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        if (!prefs.getKeysInitialized()) initApiKeys();
        applyThemeMode();
        currentPersonality = prefs.getPersonality();
        PersonalityManager.Personality p = PersonalityManager.findByName(currentPersonality, prefs);
        prefs.setSystemPrompt(p.systemPrompt);
        prefs.setTemperature(p.temperature);
        prefs.setMaxTokens(p.maxTokens);
        currentMode = prefs.getCurrentMode();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // UI-Elemente
        providerSpinner = findViewById(R.id.providerSpinner);
        modeSpinner = findViewById(R.id.modeSpinner);
        personalitySpinner = findViewById(R.id.personalitySpinner);
        chatRecycler = findViewById(R.id.chatRecycler);
        inputText = findViewById(R.id.inputText);
        sendButton = findViewById(R.id.sendButton);
        stopButton = findViewById(R.id.stopButton);
        copyButton = findViewById(R.id.copyButton);
        pasteButton = findViewById(R.id.pasteButton);
        micButton = findViewById(R.id.micButton);
        learningBtn = findViewById(R.id.learningToggleBtn);

        // Lern-Engine
        learningEngine = new LearningEngine(prefs);
        learningEngine.setSuggestionListener(new LearningEngine.SuggestionListener() {
            @Override public void onSuggestion(String category, String provider) {
                runOnUiThread(() -> {
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("🧠 Lern-Vorschlag")
                        .setMessage("Für '" + category + "'-Anfragen empfehle ich den Provider '" + provider + "'.\nMöchtest du das übernehmen?")
                        .setPositiveButton("Ja", (d, w) -> {
                            prefs.setDefaultProvider(category, provider);
                            Toast.makeText(MainActivity.this, "Provider für " + category + " gespeichert.", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Nein", null)
                        .setNeutralButton("Nicht mehr fragen", (d, w) -> {
                            prefs.setUltraAutoApply(true);
                            Toast.makeText(MainActivity.this, "Zukünftige Vorschläge automatisch übernommen.", Toast.LENGTH_SHORT).show();
                        })
                        .show();
                });
            }
            @Override public void onAutoApplied(String category, String provider) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "🧠 Automatisch: " + provider + " für " + category, Toast.LENGTH_SHORT).show());
            }
            @Override public void onKeywordListChange(String newList) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "📝 Keyword-Liste gewechselt zu: " + newList, Toast.LENGTH_LONG).show());
            }
        });

        // Lern-Button
        learningBtn.setOnClickListener(v -> toggleLearning());
        updateLearningButton();

        // Provider-Spinner
        String[] providers = ApiClient.getAllProviders();
        ArrayAdapter<String> providerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, providers);
        providerSpinner.setAdapter(providerAdapter);
        providerSpinner.setSelection(0);

        // Modus-Spinner
        String[] modes = {"Ultra", "Nitro", "Silent", "Smart"};
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, modes);
        modeSpinner.setAdapter(modeAdapter);
        int modeIdx = Arrays.asList(modes).indexOf(currentMode);
        if (modeIdx >= 0) modeSpinner.setSelection(modeIdx);
        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                currentMode = modes[pos];
                prefs.setCurrentMode(currentMode);
                Toast.makeText(MainActivity.this, "🔁 Modus: " + currentMode, Toast.LENGTH_SHORT).show();
                updateLearningButton();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Speech
        micPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) startSpeechRecognition();
            else Toast.makeText(this, "Mikrofon-Berechtigung benötigt", Toast.LENGTH_SHORT).show();
        });
        speechLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                ArrayList<String> spoken = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (spoken != null && !spoken.isEmpty()) {
                    String text = spoken.get(0);
                    String current = inputText.getText().toString();
                    String combined = current.isEmpty() ? text : current + " " + text;
                    inputText.setText(combined);
                    inputText.setSelection(combined.length());
                }
            }
        });

        chatAdapter = new ChatAdapter();
        chatRecycler.setLayoutManager(new LinearLayoutManager(this));
        chatRecycler.setAdapter(chatAdapter);
        chatAdapter.setFontMode(prefs.getBubbleFont());

        chatAdapter.setOnMessageLongPress((position, msg) -> {
            if (msg.type == ChatAdapter.TYPE_SYSTEM) return;
            List<String> options = new ArrayList<>();
            options.add("📋 Kopieren");
            options.add(prefs.isFavorite(msg.text) ? "☆ Favorit entfernen" : "⭐ Favorisieren");
            if (msg.historyIndex >= 0) options.add("🗑️ Diese Nachricht löschen");
            new AlertDialog.Builder(this).setTitle("Nachricht").setItems(options.toArray(new String[0]), (d, which) -> {
                String choice = options.get(which);
                if (choice.startsWith("📋")) {
                    android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("Copied Text", msg.text));
                    Toast.makeText(this, "📋 Kopiert!", Toast.LENGTH_SHORT).show();
                } else if (choice.startsWith("⭐")) {
                    prefs.addFavorite(msg.text);
                    Toast.makeText(this, "⭐ Zu Favoriten hinzugefügt.", Toast.LENGTH_SHORT).show();
                } else if (choice.startsWith("☆")) {
                    prefs.removeFavorite(msg.text);
                    Toast.makeText(this, "Favorit entfernt.", Toast.LENGTH_SHORT).show();
                } else if (choice.startsWith("🗑️")) {
                    int hIdx = msg.historyIndex;
                    if (hIdx >= 0 && hIdx < history.size()) {
                        history.remove(hIdx);
                        chatAdapter.removeAt(position);
                        chatAdapter.decrementHistoryIndexesAfter(hIdx);
                        saveHistory();
                        Toast.makeText(this, "Nachricht gelöscht.", Toast.LENGTH_SHORT).show();
                    }
                }
            }).show();
        });
        chatAdapter.setOnMessageClick(msg -> {
            if (!prefs.getDebugMode() || msg.debugInfo == null) return;
            new AlertDialog.Builder(this).setTitle("🐞 Debug-Info").setMessage(msg.debugInfo).setPositiveButton("OK", null).show();
        });

        if (!prefs.getCopyButton()) copyButton.setVisibility(View.GONE);

        // Scroll-Buttons
        findViewById(R.id.scrollTopBtn).setOnClickListener(v -> scrollToTop());
        findViewById(R.id.scrollMiddleBtn).setOnClickListener(v -> scrollToMiddle());
        findViewById(R.id.scrollBottomBtn).setOnClickListener(v -> scrollToBottom());

        // Persönlichkeiten laden
        loadPersonalities();
        personalitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (pos >= 0 && pos < allPers.size()) {
                    PersonalityManager.Personality sel = allPers.get(pos);
                    currentPersonality = sel.name;
                    prefs.setPersonality(sel.name);
                    prefs.setSystemPrompt(sel.systemPrompt);
                    prefs.setTemperature(sel.temperature);
                    prefs.setMaxTokens(sel.maxTokens);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        inputText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) { sendMessage(); return true; }
            return false;
        });
        sendButton.setOnClickListener(v -> sendMessage());
        stopButton.setOnClickListener(v -> stopProcessing());
        copyButton.setOnClickListener(v -> copyLastMessage());
        pasteButton.setOnClickListener(v -> {
            android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                CharSequence pasted = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
                String current = inputText.getText().toString();
                String combined = current.isEmpty() ? pasted.toString() : current + pasted.toString();
                inputText.setText(combined);
                inputText.setSelection(combined.length());
            }
        });
        micButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startSpeechRecognition();
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            }
        });

        resetInactivityTimer();
        loadHistory();
        addSystemMessage("GhostMax bereit");
        showOnboardingIfNeeded();
    }

    // === Lernen ===
    private void toggleLearning() {
        if (prefs.isLearningHardDisabled()) {
            Toast.makeText(this, "Lernen ist in den Einstellungen deaktiviert.", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean newState = !prefs.isLearningEnabled();
        prefs.setLearningEnabled(newState);
        updateLearningButton();
        Toast.makeText(this, newState ? "🧠 Lernen aktiviert" : "🧠 Lernen deaktiviert", Toast.LENGTH_SHORT).show();
    }

    private void updateLearningButton() {
        if (learningBtn == null) return;
        if (prefs.isLearningHardDisabled()) {
            learningBtn.setAlpha(0.4f);
            learningBtn.setEnabled(false);
            learningBtn.setImageResource(R.drawable.ic_brain_inactive);
        } else {
            learningBtn.setAlpha(1.0f);
            learningBtn.setEnabled(true);
            learningBtn.setImageResource(prefs.isLearningEnabled() ? R.drawable.ic_brain_active : R.drawable.ic_brain_inactive);
        }
    }

    private boolean shouldLearn() { return prefs.isUltraMode() || prefs.isLearningEnabled(); }

    // === Scroll ===
    private void scrollToTop() { chatRecycler.post(() -> { if (chatAdapter.size()>0) chatRecycler.smoothScrollToPosition(0); }); }
    private void scrollToMiddle() { chatRecycler.post(() -> { int size=chatAdapter.size(); if (size>0) chatRecycler.smoothScrollToPosition(size/2); }); }
    private void scrollToBottom() { chatRecycler.post(() -> { if (chatAdapter.size()>0) chatRecycler.smoothScrollToPosition(chatAdapter.size()-1); }); }

    // === Persönlichkeiten ===
    private void loadPersonalities() {
        allPers = new ArrayList<>();
        allPers.addAll(PersonalityManager.getDefaultPersonalities());
        allPers.addAll(PersonalityManager.loadCustom(prefs));
        List<String> persNames = new ArrayList<>();
        for (PersonalityManager.Personality per : allPers) persNames.add(per.icon + " " + per.name);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, persNames);
        personalitySpinner.setAdapter(adapter);
        int idx = -1;
        for (int i=0; i<allPers.size(); i++) if (allPers.get(i).name.equals(currentPersonality)) { idx=i; break; }
        if (idx >= 0) personalitySpinner.setSelection(idx);
    }

    // === Senden ===
    private void sendMessage() {
        if (isProcessing.get()) return;
        String userText = inputText.getText().toString().trim();
        if (userText.isEmpty()) return;
        inputText.setText("");
        history.add(new ChatMessage(ChatMessage.TYPE_USER, userText, false));
        addMessage(userText, true, null, history.size()-1);
        vibrate();
        addSystemMessage("⏳ Antwort wird geladen...");
        loadingAnimHandler.postDelayed(loadingAnimRunnable, 400);
        isProcessing.set(true);
        sendButton.setEnabled(false);
        stopButton.setEnabled(true);

        currentThread = new Thread(() -> {
            try {
                String provider = (String) providerSpinner.getSelectedItem();
                String chainCmd = prefs.getChainCommand();
                String mode = currentMode;

                // Ultra-Modus: gelernte Provider-Präferenz nutzen
                if (prefs.isUltraMode()) {
                    String category = classifyPrompt(userText);
                    String defaultProvider = prefs.getDefaultProvider(category);
                    if (defaultProvider != null && !defaultProvider.isEmpty()) {
                        provider = defaultProvider;
                    }
                }

                ApiClient.Result result = ApiClient.callWithFallback(provider, prefs, history, userText, "Chat", chainCmd);

                if (shouldLearn() && learningEngine != null) {
                    learningEngine.learn(userText, result.actualProvider, result.latency);
                }

                runOnUiThread(() -> {
                    if (!isProcessing.get()) return;
                    removeLastSystemMessage();
                    if (result.isError) {
                        addMessage("❌ Fehler: " + result.text, false, null, -1);
                    } else {
                        history.add(new ChatMessage(ChatMessage.TYPE_ASSISTANT, result.text, false));
                        addMessage(result.text, false, result.actualProvider, history.size()-1);
                        prefs.incrementProviderUsage(result.actualProvider);
                        vibrate();
                        saveHistory();
                    }
                    isProcessing.set(false);
                    sendButton.setEnabled(true);
                    stopButton.setEnabled(false);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    removeLastSystemMessage();
                    addMessage("❌ Fehler: " + e.getMessage(), false, null, -1);
                    isProcessing.set(false);
                    sendButton.setEnabled(true);
                    stopButton.setEnabled(false);
                });
            }
        });
        currentThread.start();
    }

    private String classifyPrompt(String prompt) {
        String lower = prompt.toLowerCase();
        List<String> keywords = prefs.getActiveKeywords();
        for (String keyword : keywords) {
            if (lower.contains(keyword.toLowerCase())) return "Sensitive";
        }
        if (lower.matches(".*(code|function|debug|api|sdk|library|algorithmus).*")) return "Coding";
        if (lower.matches(".*(bank|geld|überweisung|konto|finanz).*")) return "Financial";
        if (lower.matches(".*(adresse|standort|gps|route).*")) return "Location";
        if (lower.matches(".*(name|ausweis|perso|identität).*")) return "Personal";
        return "Normal";
    }

    private void stopProcessing() {
        isProcessing.set(false);
        if (currentThread != null) currentThread.interrupt();
        stopButton.setEnabled(false);
        sendButton.setEnabled(true);
        removeLastSystemMessage();
        addSystemMessage("⏹ Anfrage abgebrochen.");
    }

    private void addMessage(String text, boolean isUser, String meta, int historyIndex) {
        ChatAdapter.DisplayMessage dm = new ChatAdapter.DisplayMessage(
            isUser ? ChatAdapter.TYPE_USER : ChatAdapter.TYPE_BOT,
            text, meta, false
        );
        dm.historyIndex = historyIndex;
        chatAdapter.add(dm);
        if (prefs.getAutoScroll()) scrollToBottom();
    }

    private void addSystemMessage(String text) {
        chatAdapter.add(new ChatAdapter.DisplayMessage(ChatAdapter.TYPE_SYSTEM, text, null, false));
        if (prefs.getAutoScrollSystem()) scrollToBottom();
    }

    private void removeLastSystemMessage() {
        ChatAdapter.DisplayMessage last = chatAdapter.last();
        if (last != null && last.type == ChatAdapter.TYPE_SYSTEM && last.text.contains("wird geladen")) {
            loadingAnimHandler.removeCallbacks(loadingAnimRunnable);
            chatAdapter.removeLast();
        }
    }

    private void copyLastMessage() {
        ChatAdapter.DisplayMessage last = chatAdapter.last();
        if (last != null && last.type == ChatAdapter.TYPE_BOT) {
            android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(android.content.ClipData.newPlainText("Copied Text", last.text));
            Toast.makeText(this, "📋 Kopiert!", Toast.LENGTH_SHORT).show();
        }
    }

    // === Historie ===
    private void loadHistory() {
        try {
            String json = prefs.getHistory();
            if (json != null && !json.isEmpty() && !"[]".equals(json)) {
                org.json.JSONArray arr = new org.json.JSONArray(json);
                for (int i=0; i<arr.length(); i++) {
                    org.json.JSONObject o = arr.getJSONObject(i);
                    int type = o.getInt("type");
                    String text = o.getString("text");
                    boolean err = o.optBoolean("isError", false);
                    history.add(new ChatMessage(type, text, err));
                    if (type == ChatMessage.TYPE_USER) addMessage(text, true, null, history.size()-1);
                    else if (type == ChatMessage.TYPE_ASSISTANT) addMessage(text, false, null, history.size()-1);
                }
            }
        } catch (Exception ignored) {}
    }

    private void saveHistory() {
        try {
            org.json.JSONArray arr = new org.json.JSONArray();
            for (ChatMessage m : history) {
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("type", m.type); o.put("text", m.text); o.put("isError", m.isError); o.put("timestamp", m.timestamp);
                arr.put(o);
            }
            prefs.saveHistory(arr.toString());
        } catch (Exception ignored) {}
    }

    // === Menü ===
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem secretItem = menu.findItem(R.id.action_secret);
        if (secretItem != null) secretItem.setTitle(secretMode ? "🕵️✔️ Geheim-Modus (aktiv)" : "🕵️ Geheim-Modus");
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) { showSettingsDialog(); return true; }
        if (id == R.id.action_add_provider) { showProviderDialog(); return true; }
        if (id == R.id.action_catalog) { showModelCatalog(); return true; }
        if (id == R.id.action_add_personality) { showAddPersonalityDialog(); return true; }
        if (id == R.id.action_image) { showImageDialog(); return true; }
        if (id == R.id.action_server) { toggleLocalServer(); return true; }
        if (id == R.id.action_secret) { toggleSecretMode(); return true; }
        if (id == R.id.action_clear) { confirmClearChat(); return true; }
        if (id == R.id.action_export) { exportChatAsText(); return true; }
        if (id == R.id.action_stats) { showUsageStats(); return true; }
        if (id == R.id.action_changelog) { showChangelog(); return true; }
        if (id == R.id.action_favorites) { showFavorites(); return true; }
        if (id == R.id.action_autodialog) { toggleAutoDialog(); return true; }
        if (id == R.id.action_ghostgpt) { showGhostGptPlaceholder(); return true; }
        if (id == R.id.action_projects) { startActivity(new Intent(this, ProjectListActivity.class)); return true; }
        if (id == R.id.action_chain_command) { showChainCommandDialog(); return true; }
        if (id == R.id.action_undo) { undoLastAction(); return true; }
        if (id == R.id.action_search) { setupSearch(); return true; }
        if (id == R.id.action_backup) { backupConfig(); return true; }
        if (id == R.id.action_restore) { restoreConfig(); return true; }
        if (id == R.id.action_edit_keywords) { showKeywordListDialog(); return true; }
        return super.onOptionsItemSelected(item);
    }

    // === GhostGPT ===
    private void showGhostGptPlaceholder() {
        new AlertDialog.Builder(this).setTitle("👻 GhostGPT")
            .setMessage("GhostGPT bietet eine OpenAI-kompatible API und eine Web-Oberfläche.")
            .setPositiveButton("Web-Ansicht", (d, w) -> startActivity(new Intent(this, GhostGptWebActivity.class)))
            .setNegativeButton("API (Custom Provider)", (d, w) -> showProviderDialog())
            .setNeutralButton("Abbrechen", null)
            .show();
    }

    // === Provider Dialog ===
    private void showProviderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        EditText nameInput = new EditText(this); nameInput.setHint("Provider-Name");
        EditText urlInput = new EditText(this); urlInput.setHint("API-URL");
        EditText keyInput = new EditText(this); keyInput.setHint("API-Key");
        EditText modelInput = new EditText(this); modelInput.setHint("Modell-ID");
        layout.addView(nameInput);
        layout.addView(urlInput);
        layout.addView(keyInput);
        layout.addView(modelInput);

        builder.setTitle("Provider hinzufügen").setView(layout)
            .setPositiveButton("Speichern", (d, w) -> {
                String name = nameInput.getText().toString().trim();
                String url = urlInput.getText().toString().trim();
                String key = keyInput.getText().toString().trim();
                String model = modelInput.getText().toString().trim();
                if (name.isEmpty() || url.isEmpty() || model.isEmpty()) {
                    Toast.makeText(this, "Name, URL und Modell-ID erforderlich", Toast.LENGTH_SHORT).show();
                    return;
                }
                prefs.addCustomProvider(new CustomProvider(name, url, key, model, "chat"));
                Toast.makeText(this, "Provider " + name + " hinzugefügt", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Abbrechen", null).show();
    }

    // === Ketten-Befehl ===
    private void showChainCommandDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        input.setHint("Befehl für jede Anfrage (z.B. 'Antworte immer auf Deutsch')");
        input.setText(prefs.getChainCommand());
        builder.setTitle("⚡ Ketten-Befehl").setView(input)
            .setPositiveButton("Speichern", (d, w) -> {
                String cmd = input.getText().toString().trim();
                prefs.setChainCommand(cmd);
                Toast.makeText(this, "Ketten-Befehl gespeichert", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Abbrechen", null).show();
    }

    // === Geheim-Modus ===
    private void toggleSecretMode() {
        if (!secretMode) { activateSecretMode(); } else {
            secretMode = false;
            prefs.setSecureMode(false);
            invalidateOptionsMenu();
            Toast.makeText(this, "Geheim-Modus deaktiviert", Toast.LENGTH_SHORT).show();
        }
    }
    private void activateSecretMode() {
        secretMode = true;
        prefs.setSecureMode(true);
        invalidateOptionsMenu();
        Toast.makeText(this, "🕵️ Geheim-Modus aktiv – Verlauf wird nicht gespeichert", Toast.LENGTH_LONG).show();
    }

    // === Bildgenerierung ===
    private void showImageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        TextView label = new TextView(this); label.setText("Bildbeschreibung:");
        layout.addView(label);
        EditText promptInput = new EditText(this); promptInput.setHint("z.B. ein Fuchs im Schnee, digital art");
        layout.addView(promptInput);

        TextView provLabel = new TextView(this); provLabel.setText("Provider:");
        layout.addView(provLabel);
        Spinner providerSpinner = new Spinner(this);
        String[] imageProviders = {"OpenAI (DALL-E 3)", "Google (Imagen 3)", "Stable Diffusion (kostenlos)"};
        providerSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, imageProviders));
        layout.addView(providerSpinner);

        builder.setTitle("🖼️ Bildgenerierung").setView(layout)
            .setPositiveButton("Erzeugen", (d, w) -> {
                String prompt = promptInput.getText().toString().trim();
                if (prompt.isEmpty()) { Toast.makeText(this, "Bitte eine Beschreibung eingeben", Toast.LENGTH_SHORT).show(); return; }
                String provider = imageProviders[providerSpinner.getSelectedItemPosition()];
                String providerKey = provider.contains("OpenAI") ? "OpenAI" : provider.contains("Google") ? "Google" : "StableDiffusion";
                generateImage(prompt, providerKey);
            })
            .setNegativeButton("Abbrechen", null).show();
    }

    private void generateImage(String prompt, String provider) {
        addSystemMessage("⏳ Bild wird erzeugt mit " + provider + "...");
        new Thread(() -> {
            ApiClient.Result result = ApiClient.generateImage(prompt, provider, prefs);
            runOnUiThread(() -> {
                removeLastSystemMessage();
                if (result.isError) { addSystemMessage("❌ " + result.text); return; }
                if (result.isImage && result.imageBytes != null) {
                    String path = saveImageToGallery(result.imageBytes);
                    if (path != null) addMessage("🖼️ Bild gespeichert: " + path, false, null, -1);
                    else addSystemMessage("❌ Bild konnte nicht gespeichert werden.");
                } else {
                    addMessage(result.text, false, null, -1);
                }
            });
        }).start();
    }

    private String saveImageToGallery(byte[] imageBytes) {
        try {
            File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (dir == null) return null;
            if (!dir.exists()) dir.mkdirs();
            String filename = "ghostmax_" + System.currentTimeMillis() + ".png";
            File outFile = new File(dir, filename);
            try (FileOutputStream fos = new FileOutputStream(outFile)) { fos.write(imageBytes); }
            return outFile.getAbsolutePath();
        } catch (Exception e) { return null; }
    }

    // === Keyword-Listen ===
    private void showKeywordListDialog() {
        Map<String, List<String>> lists = prefs.getKeywordLists();
        String[] listNames = lists.keySet().toArray(new String[0]);
        String active = prefs.getActiveKeywordList();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📝 Keyword-Listen")
            .setSingleChoiceItems(listNames, Arrays.asList(listNames).indexOf(active), (d, which) -> {
                prefs.setActiveKeywordList(listNames[which]);
                Toast.makeText(this, "Aktive Liste: " + listNames[which], Toast.LENGTH_SHORT).show();
                d.dismiss();
            })
            .setPositiveButton("Neue Liste", (d, w) -> showCreateListDialog())
            .setNegativeButton("Schließen", null).show();
    }

    private void showCreateListDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        EditText nameInput = new EditText(this); nameInput.setHint("Listen-Name");
        EditText keywordsInput = new EditText(this); keywordsInput.setHint("Keywords (kommagetrennt)");
        keywordsInput.setMinLines(3);
        layout.addView(nameInput);
        layout.addView(keywordsInput);

        builder.setTitle("📝 Neue Keyword-Liste").setView(layout)
            .setPositiveButton("Speichern", (d, w) -> {
                String name = nameInput.getText().toString().trim();
                String keywordsStr = keywordsInput.getText().toString().trim();
                if (name.isEmpty() || keywordsStr.isEmpty()) {
                    Toast.makeText(this, "Name und Keywords benötigt", Toast.LENGTH_SHORT).show();
                    return;
                }
                Map<String, List<String>> lists = prefs.getKeywordLists();
                List<String> keywords = Arrays.asList(keywordsStr.split(","));
                for (int i=0; i<keywords.size(); i++) keywords.set(i, keywords.get(i).trim());
                lists.put(name, keywords);
                prefs.saveKeywordLists(lists);
                Toast.makeText(this, "Liste '" + name + "' erstellt", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Abbrechen", null).show();
    }

    // === Undo ===
    private void undoLastAction() {
        if (history.isEmpty()) {
            Toast.makeText(this, R.string.no_undo, Toast.LENGTH_SHORT).show();
            return;
        }
        ChatMessage last = history.remove(history.size() - 1);
        chatAdapter.removeLast();
        saveHistory();
        Toast.makeText(this, R.string.undo_snackbar, Toast.LENGTH_SHORT).show();
    }

    // === Suche ===
    private void setupSearch() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        input.setHint("Suche im Chat...");
        builder.setTitle("🔍 Chat durchsuchen").setView(input)
            .setPositiveButton("Suchen", (d, w) -> {
                String query = input.getText().toString().trim();
                if (query.isEmpty()) return;
                searchChat(query);
            })
            .setNegativeButton("Abbrechen", null).show();
    }

    private void searchChat(String query) {
        List<ChatAdapter.DisplayMessage> items = chatAdapter.getItemsSnapshot();
        StringBuilder results = new StringBuilder();
        int count = 0;
        for (int i=0; i<items.size(); i++) {
            if (items.get(i).text.toLowerCase().contains(query.toLowerCase())) {
                results.append("[").append(i+1).append("] ").append(items.get(i).text).append("\n\n");
                count++;
            }
        }
        if (count == 0) results.append("Keine Treffer.");
        new AlertDialog.Builder(this).setTitle("🔍 Suchergebnisse (" + count + ")")
            .setMessage(results.toString()).setPositiveButton("OK", null).show();
    }

    // === Backup / Restore ===
    private void backupConfig() {
        Toast.makeText(this, "Backup – noch nicht implementiert", Toast.LENGTH_SHORT).show();
    }
    private void restoreConfig() {
        Toast.makeText(this, "Restore – noch nicht implementiert", Toast.LENGTH_SHORT).show();
    }

    // === Statistik ===
    private void showUsageStats() {
        JSONObject usage = prefs.getProviderUsage();
        if (usage.length() == 0) {
            Toast.makeText(this, "Noch keine Statistik.", Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder sb = new StringBuilder();
        Iterator<String> keys = usage.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            sb.append(key).append(": ").append(usage.optInt(key, 0)).append("x\n");
        }
        new AlertDialog.Builder(this).setTitle("📊 Nutzungsstatistik").setMessage(sb.toString()).setPositiveButton("OK", null).show();
    }

    // === Favoriten ===
    private void showFavorites() {
        List<String> favs = prefs.getFavorites();
        if (favs.isEmpty()) {
            Toast.makeText(this, "Keine Favoriten.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] items = favs.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle("⭐ Favoriten (antippen zum Kopieren)")
            .setItems(items, (d, which) -> {
                android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                cm.setPrimaryClip(android.content.ClipData.newPlainText("Copied Text", items[which]));
                Toast.makeText(this, "📋 Kopiert!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Schließen", null).show();
    }

    // === Sonstiges ===
    private void showSettingsDialog() {
        Toast.makeText(this, "⚙️ Einstellungen – über Menü", Toast.LENGTH_SHORT).show();
    }
    private void showModelCatalog() {
        Toast.makeText(this, "📚 KI-Katalog – in der Toolbar", Toast.LENGTH_SHORT).show();
    }
    private void showAddPersonalityDialog() {
        Toast.makeText(this, "🧠 Persönlichkeit hinzufügen – in Einstellungen", Toast.LENGTH_SHORT).show();
    }
    private void toggleAutoDialog() {
        Toast.makeText(this, "🔁 Auto-Dialog – noch nicht implementiert", Toast.LENGTH_SHORT).show();
    }
    private void toggleLocalServer() {
        Toast.makeText(this, "🖥️ Server – noch nicht implementiert", Toast.LENGTH_SHORT).show();
    }
    private void exportChatAsText() {
        StringBuilder sb = new StringBuilder();
        sb.append("GhostMax Chat-Export\n====================\n\n");
        for (ChatMessage m : history) {
            String who = m.type == ChatMessage.TYPE_USER ? "Du" : "KI";
            sb.append(who).append(": ").append(m.text).append("\n\n");
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "GhostMax Chat-Export");
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(shareIntent, "Chat exportieren"));
    }
    private void showChangelog() {
        String changelog = "GhostMax v8.1\n\n- 13 KI-Provider\n- 4 Betriebsmodi\n- Projekte & Broadcast\n- Bildgenerierung (DALL-E, Imagen, Stable Diffusion)\n- Lern-Engine mit Keyword-Listen\n- Dark Mode\n- Scroll-Buttons\n- Ketten-Befehl\n- Geheim-Modus / Tor\n- Verschlüsselung\n- GhostGPT WebView\n- Undo, Suche, Favoriten, Statistik, Backup/Restore (teilweise)";
        new AlertDialog.Builder(this).setTitle("📋 Änderungsprotokoll").setMessage(changelog).setPositiveButton("OK", null).show();
    }
    private void confirmClearChat() {
        new AlertDialog.Builder(this).setTitle("Chat leeren?").setMessage("Soll der gesamte Chat gelöscht werden?")
            .setPositiveButton("Löschen", (d,w) -> { history.clear(); chatAdapter.clear(); saveHistory(); addSystemMessage("Chat geleert."); })
            .setNegativeButton("Abbrechen", null).show();
    }
    private void showOnboardingIfNeeded() {
        if (prefs.getOnboardingShown()) return;
        new AlertDialog.Builder(this).setTitle("👋 Willkommen bei GhostMax")
            .setMessage("GhostMax – KI-Assistent mit 13 Providern, 4 Modi, Projekten und Bildgenerierung.\n\n• Provider: Oben auswählen\n• Modus: Ultra, Nitro, Silent, Smart\n• Menü: Alle Einstellungen und Extras")
            .setPositiveButton("Los geht's", (d,w) -> prefs.setOnboardingShown(true)).show();
    }
    private void initApiKeys() { prefs.setKeysInitialized(true); }

    // === Vibrations ===
    private void vibrate() {
        if (prefs.getHapticFeedback()) {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) v.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    // === Theme ===
    private void applyThemeMode() {
        switch (prefs.getThemeMode()) {
            case 0: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); break;
            case 1: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
            default: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
        }
    }

    // === Inaktivität ===
    @Override public void onUserInteraction() { super.onUserInteraction(); resetInactivityTimer(); }
    @Override protected void onResume() { super.onResume(); resetInactivityTimer(); }
    private void resetInactivityTimer() {
        inactivityHandler.removeCallbacks(autoSecretRunnable);
        int minutes = prefs.getAutoSecretMinutes();
        if (minutes > 0) inactivityHandler.postDelayed(autoSecretRunnable, minutes * 60_000L);
    }

    // === Speech ===
    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Sprich jetzt...");
        try { speechLauncher.launch(intent); } catch (Exception e) {
            Toast.makeText(this, "Spracherkennung nicht verfügbar", Toast.LENGTH_SHORT).show();
        }
    }
}
