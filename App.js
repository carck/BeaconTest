import React, {Component} from 'react';
import {
    Alert,
    StyleSheet,
    Text,
    View,
    Image,
    Button,
    DeviceEventEmitter,
    PermissionsAndroid
} from 'react-native';
import Icon from 'react-native-vector-icons/Feather';

import Beacon from './Beacon';

type Props = {};

export default class App extends Component<Props> {
    constructor() {
        super();
        this.state = {console: '', distance: -1, mode: -1, meeting: false};
    }

    async componentDidMount() {
        try {
            await this.requestPermission();

            await Beacon.init();

            this.beaconListener = DeviceEventEmitter.addListener('Beacon', (e) => this.onBeacon(e));
            this.errorListener = DeviceEventEmitter.addListener('Error', (e) => this.onError(e));
        } catch (e) {
            this.onError(e);
        }
    }

    componentWillUnmount() {
        this.beaconListener && this.beaconListener.remove();
        this.errorListener && this.errorListener.remove();
        Beacon.stopListen();
        Beacon.stopBroadcast();
    }

    onBeacon(e) {
        if (e.distance <= 1) {
            if (!this.state.meeting) {
                this.setState({distance: e.distance, meeting: true});
                this.onMeeting();
            }
        } else {
            this.setState({distance: e.distance});
        }
    }

    onMeeting() {
        Alert.alert(
            'Meeting',
            'Would you like to join meeting?',
            [
                {text: 'Cancel', onPress: () => console.log('Cancel Pressed'), style: 'cancel'},
                {text: 'OK', onPress: () => console.log('OK Pressed')},
            ],
            {cancelable: false}
        )
    }

    onError(e) {
        this.setState({console: e.message});
    }

    async requestPermission() {
        return await PermissionsAndroid.request(
            "android.permission.BLUETOOTH",
            {
                'title': 'Bluetooth',
                'message': 'Bluetooth to listen for beacon'
            }
        );
    }

    runRoom() {
        Beacon.broadcast().then(() => {
            this.setState({mode: 1, console: 'broadcasting'});
        }).catch((e) => {
            Alert.alert("Alert", e.message);
        });
    }

    runClient() {
        Beacon.listen().then(() => {
            this.setState({mode: 2, console: 'listening'});
        }).catch((e) => {
            Alert.alert("Alert", e.message);
        });
    }

    render() {
        if (this.state.mode === -1) {
            return (
                <View style={styles.containerMode}>
                    <Text style={styles.welcome}>Choose Role</Text>
                    <View style={styles.mode}>
                        <View style={styles.button}>
                            <Button title={"Room"} onPress={() => this.runRoom()}/></View>
                        <View style={styles.button}>
                            <Button style={styles.button} title={"Attendee"} onPress={() => this.runClient()}/>
                        </View>
                    </View>
                </View>
            );
        }
        return (
            <View style={styles.container}>
                <View style={styles.title}>
                    <Text>Lance Zhou</Text>
                    <Icon name="settings" size={20}/>
                </View>
                <View style={styles.body}>
                    {this.state.mode === 1 ?
                        <Image style={styles.image}
                               source={require('./broadcast.gif')}/>
                        :
                        <Image style={styles.image}
                               source={require('./scan.gif')}/>
                    }
                </View>
                <View stlye={styles.bottomBar}>
                    <Text>
                        Distance: {this.state.distance}
                    </Text>
                    <Text>
                        {this.state.console}
                    </Text>
                </View>
            </View>
        );
    }
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: 'white',
    },
    containerMode: {
        flex: 1,
        backgroundColor: 'white',
        justifyContent: 'center',
    },
    mode: {
        flex: 0,
        flexDirection: 'row',
        justifyContent: 'space-around',
        alignItems: 'center',
    },
    button: {
        width: 100
    },
    image: {
        width: 300,
        height: 300,
        resizeMode: 'contain'
    },
    title: {
        flex: 1,
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        backgroundColor: 'steelblue',
    },
    body: {
        flex: 8,
        flexDirection: 'row',
        flexWrap: 'wrap',
        alignItems: 'center',
        justifyContent: 'space-around',
        alignContent: 'center',
    },
    bottomBar: {
        flex: 1
    },
    icon: {
        margin: 40
    },
    welcome: {
        fontSize: 20,
        textAlign: 'center',
        marginBottom: 60,
    }
});
