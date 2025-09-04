# Working Timer
## 概要
### モチベーション
- 所属する研究室で行われる共同研究アルバイトの労働時間の計測と，指定期間の勤務時間や給料の計算の面倒さを解消したい

### 特徴
- タイマーで計測した記録を同アプリ内でカレンダーで保持しておいて，後から自由に期間内の勤務時間を計測が可能
- 計算結果を共有できて，担当教員への連絡も簡単

### 使用技術
|分類|技術|
| :--- | :--- |
| **言語** | Kotlin |
| **UI** | Jetpack Compose |
| **アーキテクチャ** | MVVM |
| **データベース** | ・Room<br>・DataStore<br> |
| **その他** | ・Dagger Hilt<br>・Navigation Compose<br>・Kotlin Coroutines |

## 構成
```
├── data
│   ├── db
│   │   ├── AppDatabase.kt                  # Room Databaseのインスタンスを生成するクラス
│   │   ├── Work.kt                         # 作業内容を保持するエンティティ
│   │   └── WorkDao.kt                      # 作業内容のCRUDを行うDAO
│   │
│   └── repository
│       ├── DataStoreManagerImpl.kt         # DataStoreの読み書きを実装するクラス
│       ├── TimerManagerImpl.kt             # タイマーのロジックを実装するクラス
│       └── WorkRepositoryImpl.kt           # 作業内容のCRUDを実装するクラス
│
├── di
│   └── AppModule.kt                        # HiltのDIコンテナ
│
├── domain
│   └── repository
│       ├── DataStoreManager.kt             # DataStoreの読み書きのインターフェース
│       ├── TimerManager.kt                 # タイマーのロジックのインターフェース
│       └── WorkRepository.kt               # 作業内容のCRUDのインターフェース
│
├── navigation
│   ├── AppNavHost.kt                       # Navigation Composeのナビゲーションを管理するクラス
│   └── Routes.kt                           # 画面遷移のルートを定義するクラス
│
├── service
│   ├── TimerActionReceiver.kt              # タイマーの通知からアクションを受け取るBroadcastReceiver
│   └── TimerService.kt                     # タイマーを管理するService
│
├── ui
│   ├── components
│   │   ├── DataRangePickerModal.kt         # 期間指定のモーダルダイアログ
│   │   ├── FooterNavigationBar.kt          # フッターのナビゲーションバー
│   │   ├── MaterialDatePickerDialog.kt     # Material Designの日付選択ダイアログ
│   │   ├── MaterialTimePickerDialog.kt     # Material Designの時間選択ダイアログ
│   │   └── WorkItemComposable.kt           # 作業内容を表示するComposable
│   │ 
│   ├── edit_work
│   │   ├── EditWorkScreen.kt               # 作業内容の編集画面
│   │   └── EditWorkViewModel.kt            # 作業内容の編集画面のViewModel
│   │
│   ├── log_view
│   │   ├── LogViewScreen.kt                # 作業履歴の表示画面
│   │   └── LogViewViewModel.kt             # 作業履歴の表示画面のViewModel
│   │
│   └── main
│       ├── MainActivity.kt                 # アプリのエントリポイント
│       ├── MainScreen.kt                   # メイン画面
│       └── MainViewModel.kt                # メイン画面のViewModel
│
└── util
    ├── Color.kt                            # アプリで使用する色を定義する
    └── Constants.kt                        # アプリ全体で使用する定数を定義する
```

## 使い方

- スクリーンショット [docs/screenshot](https://github.com/kyo1941/working_timer/tree/main/docs/screenshot)
- 使用動画 [docs/video](https://github.com/kyo1941/working_timer/tree/main/docs/video)

### スクリーンショット
- メインタイマー画面

![image](docs/screenshot/images/timerView.png) 

---

- カレンダー画面

![image](docs/screenshot/images/calendarView.png)

---

### テスト
ユニットテスト カバレッジ
https://github.com/kyo1941/working_timer/pull/111
