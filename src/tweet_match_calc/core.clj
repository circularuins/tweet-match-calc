(ns tweet-match-calc.core
  (:require [tweet-match-calc.db.mysql :as mysql]
            [tweet-match-calc.calc.morpho :as morpho]
            [tweet-match-calc.calc.leven :as leven]
            [clojure.string :as str]))


;; API利用用twitterアカウント
(def tw-accounts (read-string (slurp "config/twitter.clj")))

;; テストデータ
(def b-1 ["kanojo_hoshi_" "eclipse_rkt" "BaskeHi" "ilikesicp" "nobkz" "takuya199850" "mug_en" "charaoyukirin" "tetsuya1975m" "doumonnd"])
(def g-1 ["kareshi_hoshi_" "namida1055" "0428hrChi" "nemukyun1" "chomado" "Zwei_Megu" "xion_2574" "ManyaRiko" "gyuunyuu_umai" "tamakazura_yuri"])


;; １ユーザー毎の、相性ランキング、頻出ワードを取得する
(defn get-analyses [user candidates twitters]
  (loop [i 0
         analyses (atom [])
         user-data (morpho/get-tweet-analyze user (nth twitters (- (count twitters) 1)))]
    (when (< i (count candidates))
      (let [twitter (nth twitters (rem i (count twitters)))
            candidate (nth candidates i)]
        (Thread/sleep 900)
        (swap! analyses conj (array-map :screen-name candidate
                                        :leven (leven/levenshtein-distance (:text user-data) (:text (morpho/get-tweet-analyze candidate twitter)))))
        (if (= (+ i 1) (count candidates))
          (println
           (array-map :screen-name user
                      :top-words (:top-words user-data)
                      :ranking (sort-by :leven @analyses)))
          (recur (inc i) analyses user-data))))))

;; 全ユーザーの解析
(defn go-matching [users candidates twitters]
  (map #(get-analyses % candidates twitters) users))
