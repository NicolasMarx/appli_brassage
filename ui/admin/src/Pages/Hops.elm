module Pages.Hops exposing (Model, Msg(..), init, update, view)

import Api
import Html exposing (Html, div, li, text, ul)
import Html.Attributes as A
import Http


-- MODEL

type alias Model =
    { loading : Bool
    , error : Maybe String
    , hops : List Api.Hop
    }


-- MSG

type Msg
    = GotHops (Result Http.Error (List Api.Hop))


-- INIT

init : ( Model, Cmd Msg )
init =
    ( { loading = True, error = Nothing, hops = [] }
    , Api.getHops Nothing GotHops
    )


-- UPDATE

update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GotHops (Ok hs) ->
            ( { model | loading = False, error = Nothing, hops = hs }, Cmd.none )

        GotHops (Err e) ->
            ( { model | loading = False, error = Just (httpErrorToString e) }, Cmd.none )


-- VIEW

view : Model -> Html Msg
view model =
    if model.loading then
        div [] [ text "Chargement des houblons…" ]

    else
        case model.error of
            Just err ->
                div [ A.class "error" ] [ text err ]

            Nothing ->
                ul [] (List.map (\h -> li [] [ text (h.id ++ " — " ++ h.name) ]) model.hops)


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
