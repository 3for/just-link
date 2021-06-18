// SPDX-License-Identifier: MIT
pragma solidity ^0.6.0;

import "./Owned.sol";
import "./TRC20Interface.sol";

/**
 * @title A trusted proxy for updating vrfCoordinator address
 * @notice This contract provides a consistent address for the
 * VRFCoordinatorInterface but delegates where it reads from to the owner, who is
 * trusted to update it.
 */
contract VRFCoordinatorProxy is Owned {
    JustMid internal justMid;
    TRC20Interface internal token;
    struct Phase {
        uint16 id;
        address vrfCoordinator;
    }
    Phase private currentPhase;
    address public proposedVrfCoordinator;
    mapping(uint16 => address) public phaseVrfCoordinators;

    uint256 constant private PHASE_OFFSET = 64;
    uint256 constant private PHASE_SIZE = 16;
    uint256 constant private MAX_ID = 2**(PHASE_OFFSET+PHASE_SIZE) - 1;

    constructor(address _vrfCoordinator, address _jst, address _justMid) public Owned() {
        token = TRC20Interface(_jst);
        justMid = JustMid(_justMid);
        setVrfCoordinator(_vrfCoordinator);
    }

    /**
   * @notice Called by LINK.transferAndCall, on successful LINK transfer
   *
   * @dev To invoke this, use the requestRandomness method in VRFConsumerBase.
   *
   * @dev The VRFCoordinator will call back to the calling contract when the
   * @dev oracle responds, on the method fulfillRandomness. See
   * @dev VRFConsumerBase.fulfilRandomness for its signature. Your consuming
   * @dev contract should inherit from VRFConsumerBase, and implement
   * @dev fulfilRandomness.
   *
   * @param _sender address: who sent the LINK (must be a contract)
   * @param _fee amount of LINK sent
   * @param _data abi-encoded call to randomnessRequest
   */
    function onTokenTransfer(address _sender, uint256 _fee, bytes memory _data)
    public
    {
        assembly { // solhint-disable-line no-inline-assembly
            mstore(add(_data, 36), _sender) // ensure correct sender is passed
            mstore(add(_data, 68), _fee) // ensure correct amount is passed
        }
        token.approve(justMidAddress(), _fee);
        require(justMid.transferAndCall(address(this), getVrfCoordinator(), _fee, _data), "unable to transferAndCall to vrfCoordinator");
    }

    /**
     * @notice Retrieves the stored address of the LINK token
     * @return The address of the LINK token
     */
    function justMidAddress()
    public
    view
    returns (address)
    {
        return address(justMid);
    }

    /**
     * @notice returns the current phase's aggregator address.
     */
    function getVrfCoordinator()
    public
    view
    returns (address)
    {
        return address(currentPhase.vrfCoordinator);
    }

    /**
     * @notice returns the current phase's ID.
     */
    function phaseId()
    external
    view
    returns (uint16)
    {
        return currentPhase.id;
    }

    /**
     * @notice Allows the owner to propose a new address for the aggregator
     * @param _vrfCoordinator The new address for the aggregator contract
     */
    function proposeVrfCoordinator(address _vrfCoordinator)
    external
    onlyOwner()
    {
        proposedVrfCoordinator = _vrfCoordinator;
    }

    /**
     * @notice Allows the owner to confirm and change the address
     * to the proposed aggregator
     * @dev Reverts if the given address doesn't match what was previously
     * proposed
     * @param _vrfCoordinator The new address for the aggregator contract
     */
    function confirmVrfCoordinator(address _vrfCoordinator)
    external
    onlyOwner()
    {
        require(_vrfCoordinator == address(proposedVrfCoordinator), "Invalid proposed aggregator");
        delete proposedVrfCoordinator;
        setVrfCoordinator(_vrfCoordinator);
    }


    /*
     * Internal
     */
    function setVrfCoordinator(address _vrfCoordinator)
    internal
    {
        uint16 id = currentPhase.id + 1;
        currentPhase = Phase(id, _vrfCoordinator);
        phaseVrfCoordinators[id] = _vrfCoordinator;
    }


}