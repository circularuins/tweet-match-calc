(ns tweet-match-calc.calc.morpho
  (:require [tweet-match-calc.api.twitter :as twitter]
            [clojure.string :as str])
  (:import [org.atilika.kuromoji Token Tokenizer]))


;; フィルタリングワード（名詞）
(def filt-nouns
  ["こと" "それ" "いま" "よう" "ところ" "とこ" "ため" "そう" "ほう" "もの" "これ" "感じ" "とき" "ここ" "あと" "だれ" "いつ" "ぼく" "僕" "わたし" "私" "さん" "ちゃん" "くん" "せい" "みたい" "どこ" "昨日" "今日" "明日" "！@" "？@"
   ])
;; フィルタリングワード（動詞）
(def filt-verbs
  ["いる" "てる" "する" "なる" "ある" "あっ" "やっ" "なっ" "られ" "れる" "られる" "くる" "ゆう" "思う" "思っ"
   ])



;;; フィルタ関数

;; 名詞
(defn noun? [parts] (= "名詞" parts))
;; 動詞
(defn verb? [parts] (= "動詞" parts))
;; 数字
(defn str-number? [parts]
  (try
    (Integer/parseInt parts)
    true
    (catch Exception e (str "caught exception: " (.getMessage e)))))
;; 空
(def not-nil? (complement nil?))
;; 一文字単語
(defn single-char? [word] (not (= 1 (count word))))
;; 半角のみの文字列かどうか
(defn hankaku-only? [word]
  (if (nil? word)
    true
    (if (re-matches #"^[a-zA-Z0-9 -/:-@\[-\`\{-\~]+$" word)
      false
      true)))
(defn www? [word]
  (if (re-matches #"^[ｗ|？|！|ー|～]+$" word)
    false
    true))

;; フィルタリング（名詞）
(defn filtering-nouns? [word]
  (not (some #(= % word) filt-nouns)))
;; フィルタリング（動詞）
(defn filtering-verbs? [word]
  (not (some #(= % word) filt-verbs)))
;; "彼女"または"彼氏" => "彼"へ変換
(defn convert-kanojo-kareshi [word]
  (if (= word "彼女")
    "彼"
    (if (= word "彼氏")
      "彼"
      word)))



;;; 形態素解析関数

;; 名詞を抜き出す
(defn select-noun
  [sentence]
  (try
    (let [^Tokenizer tokenizer (.build (Tokenizer/builder))]
      (->>
       (filter not-nil?
               (for [^Token token (.tokenize tokenizer sentence)]
                 (if (some noun? (str/split (.getPartOfSpeech token) #","))
                   (.getSurfaceForm token))))
       (filter single-char?)
       (filter filtering-nouns?)
       (filter hankaku-only?)
       (filter www?)
       (map #(if (not (= true (str-number? %))) %))
       (map convert-kanojo-kareshi)
       ))
    (catch NullPointerException e (str "caught exception: " (.getMessage e)))))

;; 動詞を抜き出す
(defn select-verb
  [sentence]
  (try
    (let [^Tokenizer tokenizer (.build (Tokenizer/builder))]
      (->>
       (filter not-nil?
               (for [^Token token (.tokenize tokenizer sentence)]
                 (if (some verb? (str/split (.getPartOfSpeech token) #","))
                   (.getSurfaceForm token))))
       (filter single-char?)
       (filter filtering-verbs?)
       (filter hankaku-only?)
       (map #(if (not (= true (str-number? %))) %))
       ))
    (catch NullPointerException e (str "caught exception: " (.getMessage e)))))



;;; 集計関数

;; ワードカウント
(defn word-count [words]
  (reduce (fn [words word] (assoc words word (inc (get words word 0))))
          {}
          words))

;; 名詞の集計
(defn count-noun-freq [data]
  (reverse (sort-by second (word-count (select-noun (:text data))))))

;; 動詞の集計
(defn count-verb-freq [data]
    (reverse (sort-by second (word-count (select-verb (:text data))))))



;;; テキスト整形

;; 名詞の整形
(defn noun-text [words]
  (->>
   (map #(str/join (repeat (nth % 1) (nth % 0))) (take 200 words))
   (str/join)
   (take 500)
   (str/join)))

;; 動詞の整形
(defn verb-text [words]
  (->>
   (map #(str/join (repeat (nth % 1) (nth % 0))) (take 100 words))
   (str/join)
   (take 300)
   (str/join)))

;; top5ワードを返す
(defn get-top-words [words]
  (->>
   (take 5 words)
   (map #(nth % 0))))

;; 最終的に欲しいマップを返す
(defn get-tweet-analyze [user-id auth]
  (let [tw-result (twitter/get-tweets user-id auth)]
    (let [n-words (count-noun-freq tw-result)
          v-words (count-verb-freq tw-result)]
      (array-map
       :screen_name (:screen-name tw-result)
       :profile-image (:profile-image tw-result)
       :profile-back-url (:profile-back-url tw-result)
       :user-name (:user-name tw-result)
       :text (str (noun-text n-words) (verb-text v-words))
       :top-words (get-top-words n-words)))))
