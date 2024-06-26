package backend.service;

import backend.config.JwtService;
import backend.config.TradePetitionHelper;
import backend.models.*;
import backend.models.dtoResponse.DtoTradePetition_o;
import backend.models.enums.Role;
import backend.models.enums.SkinCondition;
import backend.repository.TradePetitionRepository;
import backend.repository.TradePetitionRepositoryc;
import backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Optional;

import static backend.utils.UtilMethods.getPageable;
import static backend.utils.UtilMethods.tryParsePageNumber;

@Service
public class TradePetitionService {

    private final TradePetitionHelper tradePetitionHelper;
    private final TradePetitionRepository tradePetitionRepository;
    private final TradePetitionRepositoryc tradePetitionRepositoryc;
    private final SkinService skinService;
    private final StickerService stickerService;
    private final CrateService crateService;
    private final int MAX_TRADE_PETITIONS_PER_PAGE = 15;

    @Autowired
    public TradePetitionService(TradePetitionRepository tradePetitionRepository,
                                TradePetitionHelper tradePetitionHelper,
                                TradePetitionRepositoryc tradePetitionRepositoryc,
                                SkinService skinService,
                                StickerService stickerService,
                                CrateService crateService){
        this.tradePetitionRepository = tradePetitionRepository;
        this.tradePetitionHelper = tradePetitionHelper;
        this.tradePetitionRepositoryc = tradePetitionRepositoryc;
        this.skinService = skinService;
        this.stickerService = stickerService;
        this.crateService = crateService;
    }

    /**
     * Gets a max of 15 trade petitions
     * @param page number of the page of the trade petitions
     * @return page of 15 trade petitions
     */
    public Page<DtoTradePetition_o> getAllUnfiltered(String page) throws Exception {
        int pageNumber = tryParsePageNumber(page, 0);
        Pageable pageable = PageRequest.of(pageNumber, MAX_TRADE_PETITIONS_PER_PAGE, Sort.by("creation_ms").descending());
        Page<TradePetition> tradePetitions = tradePetitionRepository.getAllTradePetitions(pageable);
        // Parse the pure trade petitions to the DtoTradePetition
        return tradePetitions.map(tradePetitionHelper::tradePetitionToDTO);
    }

    /**
     * Takes a map of parameters and returns a page of TradePetitions
     * @param parameters map of parameters
     * @return page of the data transfer object TradePetition
     * @throws Exception when the parameters of the Tradepeetition are wrong
     */
    public Page<DtoTradePetition_o> getTradePetitionsFiltered(HashMap<String, String> parameters) throws Exception {
        String search_type = null;
        Boolean parsed_search_type = null;
        String item_type = null;
        String name = null;
        String weapon = null;
        String rarity = null;
        String condition = null;
        int pattern = -1;
        boolean stattrak = false;
        boolean souvenir = false;
        boolean special = false;
        int page = 0;
        String sortAttribute = null;
        String direction = null;

        final Pageable pageable;
        // TradePetitions page that will be returned
        Page<DtoTradePetition_o> tradePetitionsDto;

        if (parameters.size() == 0) {
            pageable = PageRequest.of(0, MAX_TRADE_PETITIONS_PER_PAGE);
            return tradePetitionRepository.getAllTradePetitions(pageable).map(tradePetitionHelper::tradePetitionToDTO);
        }

        // If the parameter exists -> Underscores to spaces -> Assign to the variable.
        if (parameters.containsKey("search_type")) {
            search_type = parameters.get("search_type").replaceAll("_", " ").toLowerCase();
            if (search_type.equalsIgnoreCase("petition")) {
                parsed_search_type = false;
            } else if (search_type.equalsIgnoreCase("offer")) {
                parsed_search_type = true;
            }
        }
        if (parameters.containsKey("item_type")){
            item_type = parameters.get("item_type").replaceAll("_", " ").toLowerCase();
        }
        if (parameters.containsKey("name")){
            name = parameters.get("name").replaceAll("_", " ").toLowerCase();
        }
        if (parameters.containsKey("weapon")){
            weapon = parameters.get("weapon").replaceAll("_", " ");         // I don't need it lowered because
        }
        if (parameters.containsKey("rarity")){
            rarity = parameters.get("rarity").replaceAll("_", " ");         // these parameters came from a select in
        }
        if (parameters.containsKey("condition")){
            condition = parameters.get("condition").replaceAll("_", " ");// the frontend.
        }
        if (parameters.containsKey("pattern")){
            pattern = Integer.parseInt(parameters.get("pattern"));
        }
        if (parameters.containsKey("stattrak")){
            stattrak = Boolean.parseBoolean(parameters.get("stattrak"));
        }
        if (parameters.containsKey("souvenir")){
            souvenir = Boolean.parseBoolean(parameters.get("souvenir"));
        }
        if (parameters.containsKey("special")){
            special = Boolean.parseBoolean(parameters.get("special"));
        }
        if (parameters.containsKey("page")){
            page = tryParsePageNumber(parameters.get("page"), 0);
        }
        if (parameters.containsKey("sortAttribute")){
            sortAttribute = parameters.get("sortAttribute").replaceAll("_", " ").toLowerCase();
        }
        if (parameters.containsKey("direction")){
            direction = parameters.get("direction").replaceAll("_", " ").toLowerCase();
        }

        pageable = getPageable(page, MAX_TRADE_PETITIONS_PER_PAGE, sortAttribute, direction);

        if (item_type == null) {
            tradePetitionsDto = getAllUnfiltered(String.valueOf(page));
        } else if (item_type.equalsIgnoreCase("SKIN")) {
            tradePetitionsDto = tradePetitionRepository.getTradePetitionsWeapon(parsed_search_type, name, weapon, rarity, condition, pattern, stattrak, souvenir, special, pageable).map(tradePetitionHelper::tradePetitionToDTO);
        } else if (item_type.equalsIgnoreCase("CRATE")) {
            tradePetitionsDto = tradePetitionRepository.getTradePetitionsCrate(parsed_search_type, name, pageable).map(tradePetitionHelper::tradePetitionToDTO);
        } else if (item_type.equalsIgnoreCase("STICKER")) {
            tradePetitionsDto = tradePetitionRepository.getTradePetitionsSticker(parsed_search_type, name, pageable).map(tradePetitionHelper::tradePetitionToDTO);
        } else {
            throw new Exception("No se pudo procesar la petición.");
        }
        return tradePetitionsDto;
    }

    /**
     * Validates and persists a trade petition
     * @param tradePetition the tradePetition
     * @throws Exception
     */
    public void createTradePetition(TradePetition tradePetition) throws Exception {
        checkUserRate(tradePetition);
        validateTradePetition(tradePetition);
        tradePetitionRepositoryc.insertWithQuery(tradePetition);
    }

    /**
     * Updates an existing trade petition
     * @param tradePetition trade petition
     */
    public void updateTradePetition(TradePetition tradePetition) throws Exception {
        // In the tradePetition object I have the user that made the request, not the persisted
        // so I have to check to database
        checkUserUpdatePermissions(tradePetition);
        validateTradePetition(tradePetition);
        tradePetitionRepositoryc.updateWithQuery(tradePetition);

    }

    /**
     *
     * @param tradePetition the tradePetition to delete with the user who created it
     * @param jwt the json web token that has the user that it's performing the request
     * @throws Exception
     */
    public void deleteTradePetition(TradePetition tradePetition, String jwt) throws Exception {
        // In the tradePetition object I have the user that made the request, not the persisted
        // so I have to check to database
        checkUserDeletePermissions(tradePetition, jwt);
        tradePetitionRepositoryc.deleteWithQuery(tradePetition);
    }

    /**
     * An user can only update a tradePetition if its the owner or if it's an admin user
     * @param tradePetition the tradePetition
     * @throws IllegalStateException if the user does not have permission to update the tradePetition
     */
    private void checkUserUpdatePermissions(TradePetition tradePetition) throws IllegalStateException {
        User userOfTheRequest = tradePetition.getUser();
        User userOwnerOfTheTradePetition = tradePetitionRepository.getUser(tradePetition.getId());
        if(userOfTheRequest.getId().equals(userOwnerOfTheTradePetition.getId())  || Role.ADMIN.equals(userOfTheRequest.getRole())){
            return; // Means the user is allowed to delete the tradePetition
        }
        throw new IllegalStateException("El usuario " + userOfTheRequest.getUsername() + " no tiene permisos de modificación");
    }

    /**
     * Validates if an user is able to create trade petitions
     * @param tradePetition the tradePetition
     */
    private void checkUserRate(TradePetition tradePetition) {
        final int MAX_TRADE_PETITIONS_BY_USER = 10;
        User tradePetitionUser = tradePetition.getUser();
        int userTradePetitions = countByUser(tradePetitionUser);
        if(userTradePetitions > MAX_TRADE_PETITIONS_BY_USER){
            throw new IllegalStateException("El usuario: " + tradePetitionUser.getNickname() + " ha superado la cantidad de peticiones de intercambio maximas");
        }
    }

    /**
     * Counts the amount of trade petitions that an user has created
     * @param tradePetitionUser the tradePetition
     * @return amount of trade petitions created
     */
    private int countByUser(User tradePetitionUser) {
        return tradePetitionRepository.countByUser(tradePetitionUser.getId());
    }

    /**
     * Validates that a trade petition adheres to the business rules
     * @param tradePetition trade petition that comes from the json parse of the request's body
     * @throws Exception if the tradePetition is not valid according to the busniness rules
     */
    public void validateTradePetition(TradePetition tradePetition) throws Exception {
        final int MAX_REQUESTED_ITEMS = 4;
        final int MAX_OFFERED_ITEMS = 4;
        final int MAX_STICKERS_BY_SKIN = 4;
        int requestedCounter = 0;
        int offeredCounter = 0;
        if(tradePetition.getUser() == null){
            throw new InvalidPropertiesFormatException("Falta el campo Usuario en la peticion de intercambio");
        }

        for(RequestedSkin requestedSkin : tradePetition.getRequestedSkins()){
            if(requestedSkin.getSkin().getId() == null || requestedSkin.getCondition() == null ||
               requestedSkin.getStattrak() == null || requestedSkin.getSouvenir() == null || requestedSkin.getTradeType() == null){
                throw new InvalidPropertiesFormatException("Faltan campos obligatorios en: Skin");
            }
            if(!skinService.isValidSkinId(requestedSkin.getSkin().getId())){
                throw new InvalidPropertiesFormatException("ID de Skin no valido: " + requestedSkin.getSkin().getId());
            }
            if(!SkinCondition.isValidValue(requestedSkin.getCondition())){
                throw new InvalidPropertiesFormatException("Condicion de Skin no valida: " + requestedSkin.getCondition());
            }
            if(requestedSkin.getFloatValue() != null && (requestedSkin.getFloatValue() < 0 || requestedSkin.getFloatValue() > 1)){
                throw new InvalidPropertiesFormatException("El rango del float de una Skin debe estar entre 0 y 1");
            }
            int stickerCounter = 0;
            for(Sticker sticker : requestedSkin.getStickers()){
                if(!stickerService.isValidStickerId(sticker.getId())){
                    throw new InvalidPropertiesFormatException("ID de Sticker no valido: " + sticker.getId());
                }
                stickerCounter++;
            }
            if(stickerCounter > MAX_STICKERS_BY_SKIN){
                throw new InvalidPropertiesFormatException("Hay demasiados stickers en: Skin");
            }
            if(requestedSkin.getTradeType()){
                offeredCounter++;
            }else{
                requestedCounter++;
            }
        }

        for(RequestedSticker requestedSticker : tradePetition.getRequestedStickers()){
            if(requestedSticker.getTradeType() == null || requestedSticker.getSticker().getId() == null){
                throw new InvalidPropertiesFormatException("Faltan campos obligatorios en: Sticker");
            }
            if(!stickerService.isValidStickerId(requestedSticker.getSticker().getId())){
                throw new InvalidPropertiesFormatException("ID de Sticker no valido: " + requestedSticker.getSticker().getId());
            }
            if(requestedSticker.getTradeType()){
                offeredCounter++;
            }else{
                requestedCounter++;
            }
        }

        for(RequestedCrate requestedCrate : tradePetition.getRequestedCrates()){
            if(requestedCrate.getTradeType() == null || requestedCrate.getCrate().getId() == null){
                throw new InvalidPropertiesFormatException("Faltan campos obligatorios en: Caja");
            }
            if(!crateService.isValidCrateId(requestedCrate.getCrate().getId())){
                throw new InvalidPropertiesFormatException("ID de Caja no valido: " + requestedCrate.getCrate().getId());
            }
            if(requestedCrate.getTradeType()){
                offeredCounter++;
            }else{
                requestedCounter++;
            }
        }

        int moneyOffers = 0;
        int moneyRequests = 0;
        for(MoneyPetition moneyPetition : tradePetition.getMoneyOffers()){
            if(moneyPetition.getAmount() == null || moneyPetition.getCountryCode() == null || moneyPetition.getTradeType() == null){
                throw new InvalidPropertiesFormatException("Faltan campos obligatorios en: Oferta de dinero");
            }
            if(moneyPetition.getTradeType()){
                moneyOffers++;
            }else{
                moneyRequests++;
            }
        }

        if(moneyOffers > 1 || moneyRequests > 1){
            throw new InvalidPropertiesFormatException("Cantidad de ofertas de dinero no válidas");
        }
        if(requestedCounter == 0 && moneyRequests == 0){
            throw new InvalidPropertiesFormatException("No puede existir una petición de intercambio que no pida nada");
        }
        if(offeredCounter == 0 && moneyOffers == 0){
            throw new InvalidPropertiesFormatException("No puede existir una petición de intercambio que no ofrezca nada");
        }
        if(requestedCounter > MAX_REQUESTED_ITEMS){
            throw new InvalidPropertiesFormatException("No se pueden pedir más de " + MAX_REQUESTED_ITEMS + " items.");
        }
        if(offeredCounter > MAX_OFFERED_ITEMS){
            throw new InvalidPropertiesFormatException("No se pueden pedir más de " + MAX_REQUESTED_ITEMS + " items.");
        }

    }

    public Optional<TradePetition> checkIfTradePetitionIdExists(Long tradePetitionId){
        return tradePetitionRepository.findById(tradePetitionId);
    }

    /**
     * An user can only update a tradePetition if its the owner or if it's an admin user
     * @param tradePetition the tradePetition
     * @throws IllegalStateException if the user does not have permission to update the tradePetition
     */
    private void checkUserDeletePermissions(TradePetition tradePetition, String jwt) throws IllegalStateException {
        User tradePetitionUser = tradePetition.getUser();
        User requestUser = tradePetitionHelper.getUserFromJwt(jwt);
        if(requestUser.getId().equals(tradePetitionUser.getId())  || Role.ADMIN.equals(requestUser.getRole())){
            return; // Means the user is allowed to delete the tradePetition
        }
        throw new IllegalStateException("El usuario " + requestUser.getUsername() + " no tiene permisos para eliminar");
    }
}
