(ns db-sample.core
  (:require [clj-time.core :as t]
            [clj-time.local :as tl]
            [clj-time.format :as tf]
            [clojure.string :as str])
  (:import [twitter4j conf.ConfigurationBuilder TwitterFactory Twitter Query TwitterException Paging]
           [org.atilika.kuromoji Token Tokenizer]))

(def custom-formatter (tf/formatter "yyyy/MM/dd HH:mm:ss"))
(def minutes-ago (t/minus (tl/local-now) (t/minutes 1)))


;;;;; データ取得

(use 'korma.db)

(defdb kanojodb
  (mysql
   {:db "twitter_bot"
    :port "3306"
    :user "wake"
    :password "wakenatsuhiko"}))

(use 'korma.core)

(defentity kanojo_hoshi)

(defn select-by-name [name]
  (select kanojo_hoshi
          (fields :name :screen_name :user_id :date)
          (where {:name name})
          (order :date :DESC)))

(defn select-from-now []
  (select kanojo_hoshi
          (fields :name :screen_name :user_id :date)
          (where (> :date (.toString minutes-ago)))
          (order :date :DESC)
          (limit 3)))





;;;;; ツイート取得

;; consumer-key, consumer-secret, access-token, access-token-secret の読み込み
(def auth-data (read-string (slurp "config/twitter.clj")))

(defn make-config []
  (let [config (new ConfigurationBuilder)]
    (. config setOAuthConsumerKey (auth-data :consumer-key))
    (. config setOAuthConsumerSecret (auth-data :consumer-secret))
    (. config setOAuthAccessToken (auth-data :access-token))
    (. config setOAuthAccessTokenSecret (auth-data :access-token-secret))
    (. config build)))

(defn make-twitter []
  (let [config (make-config)
        factory (new TwitterFactory config)]
    (. factory getInstance)))

(def paging
   (Paging. (int 1) (int 100)))

(defn get-tweets [screen-name]
  (let [twitter (make-twitter)
        tweets (.getUserTimeline twitter screen-name paging)]
    (->>
     tweets
     (map #(.getText %))
     str/join)))






;;;;; 形態素解析

(defn noun? [parts] (= "名詞" parts)) ;名詞かどうか
(defn verb? [parts] (= "動詞" parts)) ;動詞かどうか
(defn str-number? [parts]
  (try
    (Integer/parseInt parts)
    true
    (catch Exception e (str "caught exception: " (.getMessage e)))))
(def not-nil? (complement nil?)) ;フィルタ用
(def filt-nouns ["co" "http" "https" "://" "://..." ".@" "..@" "...@" "！#" "！@" ":)" "___" "_:" "RT" "RTRT" "ERROR" "CRITICAL" "Twitter" "in" "by" "こと" "それ" "いま" "よう" "ところ" "とこ" "ため" "そう" "ほう" "もの" "これ" "感じ" "とき" "ここ" "あと" "だれ" "いつ" "ぼく" "僕" "わたし" "私" "さん" "ちゃん" "くん" "せい" "みたい" "どこ" "??" "???" "？？" "？？？"])
(def filt-verbs [""])
(defn single-char? [word] (not (= 1 (count word))))
(defn filtering-nouns? [word]
  (not (some #(= % word) filt-nouns)))
(defn filtering-verbs? [word]
  (not (some #(= % word) filt-verbs)))
(defn convert-kanojo-kareshi [word]
  (if (= word "彼女")
    "彼"
    (if (= word "彼氏")
      "彼"
      word)))

(defn select-noun
  [sentence]
  (let [^Tokenizer tokenizer (.build (Tokenizer/builder))]
    (->>
     (filter not-nil?
             (for [^Token token (.tokenize tokenizer sentence)]
               (if (some noun? (str/split (.getPartOfSpeech token) #","))
                 (.getSurfaceForm token))))
;     (distinct)
     (filter single-char?)
     (filter filtering-nouns?)
     (map #(if (not (= true (str-number? %))) %))
     (map convert-kanojo-kareshi)
     )))

(defn select-verb
  [sentence]
  (let [^Tokenizer tokenizer (.build (Tokenizer/builder))]
    (->>
     (filter not-nil?
             (for [^Token token (.tokenize tokenizer sentence)]
               (if (some verb? (str/split (.getPartOfSpeech token) #","))
                 (.getSurfaceForm token))))
;     (distinct)
     (filter single-char?)
     (filter filtering-verbs?)
     (map #(if (not (= true (str-number? %))) %))
     )))

;; ワードカウント
(defn word-count [words]
  (reduce (fn [words word] (assoc words word (inc (get words word 0))))
          {}
          words))

;; 頻度順に集計
(defn count-noun-freq [screen-name]
  (reverse (sort-by second (word-count (select-noun (get-tweets screen-name))))))
(defn count-verb-freq [screen-name]
    (reverse (sort-by second (word-count (select-verb (get-tweets screen-name))))))

;; 文字列比較のための文字列作成
(defn noun-text [screen-name]
  (->>
   (map #(str/join (repeat (nth % 1) (nth % 0))) (take 200 (count-noun-freq screen-name)))
   (str/join)
   (take 1000)
   (str/join)))

;;　テストデータ
(def n-1 (str/join (select-noun (get-tweets "tamakazura_yuri"))))
(def n-2 (str/join (select-noun (get-tweets "doumonnd"))))
(def v-1 (str/join (select-verb (get-tweets "tamakazura_yuri"))))
(def v-2 (str/join (select-verb (get-tweets "doumonnd"))))




;;;;; レーベンシュタイン距離の計算
(defn levenshtein-distance [x y]
  (last
   (reduce
    (fn [prev j]
      (reduce
       (fn [curr i]
         (conj curr
               (+ 1 (min (nth prev (+ i 1))
                         (nth curr i)
                         (- (nth prev i)
                            (if (= (nth x i) (nth y j)) 1 0))))))
       [(+ j 1)] (range (count x))))
    (range (+ 1 (count x)))
    (range (count y)))))

(defn get-leven-noun [user-x user-y]
  (levenshtein-distance (noun-text user-x) (noun-text user-y)))
