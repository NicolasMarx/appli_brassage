module Pages.Upload exposing (Model, Msg(..), init, update, view)

import Api
import File exposing (File)
import File.Select as FileSelect
import Html exposing (Html, button, div, h2, p, span, text)
import Html.Attributes as A
import Html.Events as E
import Http


-- MODEL

type alias Model =
    { csrf : Maybe String
    , selected : Maybe File
    , filename : Maybe String
    , busy : Bool
    , error : Maybe String
    , done : Bool
    }


init : Maybe String -> ( Model, Cmd Msg )
init csrf =
    ( { csrf = csrf
      , selected = Nothing
      , filename = Nothing
      , busy = False
      , error = Nothing
      , done = False
      }
    , Cmd.none
    )


-- MSG

type Msg
    = Pick
    | Picked File
    | DoUpload
    | GotUpload (Result Http.Error Api.UploadResult)


-- UPDATE

update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        Pick ->
            ( model
            , FileSelect.file [ "text/csv", ".csv" ] Picked
            )

        Picked file ->
            ( { model
                | selected = Just file
                , filename = Just (File.name file)
                , error = Nothing
                , done = False
              }
            , Cmd.none
            )

        DoUpload ->
            case model.selected of
                Nothing ->
                    ( { model | error = Just "Veuillez choisir un fichier CSV." }, Cmd.none )

                Just f ->
                    ( { model | busy = True, error = Nothing, done = False }
                    , Api.uploadHopsCsvFile f model.csrf GotUpload
                    )

        GotUpload (Ok _) ->
            ( { model | busy = False, done = True }, Cmd.none )

        GotUpload (Err e) ->
            ( { model | busy = False, error = Just (httpErrorToString e) }, Cmd.none )


-- VIEW

view : Model -> Html Msg
view model =
    div [ A.class "card" ]
        [ h2 [] [ text "Upload CSV des houblons" ]
        , p [] [ text "Sélectionnez un fichier .csv et lancez l’import." ]
        , div [ A.style "display" "flex", A.style "gap" "12px", A.style "align-items" "center" ]
            [ button
                [ A.type_ "button", E.onClick Pick, A.disabled model.busy ]
                [ text "Choisir un fichier" ]
            , case model.filename of
                Nothing ->
                    span [ A.class "muted" ] [ text "Aucun fichier choisi" ]

                Just n ->
                    span [] [ text n ]
            ]
        , div [ A.style "margin-top" "12px" ]
            [ button
                [ A.type_ "button"
                , E.onClick DoUpload
                , A.disabled (model.busy || model.selected == Nothing)
                ]
                [ text (if model.busy then "Import…" else "Importer") ]
            ]
        , case model.error of
            Just e ->
                div [ A.class "error", A.style "margin-top" "8px" ] [ text e ]

            Nothing ->
                if model.done then
                    div [ A.class "success", A.style "margin-top" "8px" ] [ text "Import réussi" ]
                else
                    text ""
        ]


-- HELPERS

httpErrorToString : Http.Error -> String
httpErrorToString err =
    case err of
        Http.BadUrl _ ->
            "URL invalide"

        Http.Timeout ->
            "Timeout"

        Http.NetworkError ->
            "Erreur réseau"

        Http.BadStatus code ->
            "Erreur serveur (" ++ String.fromInt code ++ ")"

        Http.BadBody _ ->
            "Réponse illisible"
