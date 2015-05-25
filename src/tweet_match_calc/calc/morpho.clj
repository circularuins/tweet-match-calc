(ns tweet-match-calc.calc.morpho
  (:require [tweet-match-calc.api.twitter :as twitter]
            [clojure.string :as str])
  (:import [org.atilika.kuromoji Token Tokenizer]))

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
  (reverse (sort-by second (word-count (select-noun (twitter/get-tweets screen-name))))))
(defn count-verb-freq [screen-name]
    (reverse (sort-by second (word-count (select-verb (twitter/get-tweets screen-name))))))

;; 文字列比較のための文字列作成
(defn noun-text [screen-name]
  (->>
   (map #(str/join (repeat (nth % 1) (nth % 0))) (take 200 (count-noun-freq screen-name)))
   (str/join)
   (take 1000)
   (str/join)))
